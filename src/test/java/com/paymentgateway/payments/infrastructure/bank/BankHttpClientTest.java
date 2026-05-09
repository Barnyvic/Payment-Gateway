package com.paymentgateway.payments.infrastructure.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BankHttpClientTest {

    private MockRestServiceServer server;
    private BankHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost:8787").build();
        client = new BankHttpClient(restClient, new BankErrorMapper(new ObjectMapper()));
    }

    @Test
    void authorize_shouldCallAuthorizationEndpoint() {
        server.expect(requestTo("http://localhost:8787/api/v1/authorizations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "idem-auth-1"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"authorizationId":"auth_123","status":"AUTHORIZED"}
                                """));

        BankAuthorizeResponse response = client.authorize(
                new BankAuthorizeRequest("4111111111111111", "123", "12", "2030", 5000L, "USD"), "idem-auth-1");

        assertEquals("auth_123", response.authorizationId());
        assertEquals("AUTHORIZED", response.status());
        server.verify();
    }

    @Test
    void capture_shouldMapBankErrors() {
        server.expect(requestTo("http://localhost:8787/api/v1/captures"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":"invalid_state","message":"Authorization not capturable"}
                                """));

        BankClientException ex = assertThrows(
                BankClientException.class,
                () -> client.capture(new BankCaptureRequest("auth_123", 5000L, "USD"), "idem-cap-1"));

        assertEquals("invalid_state", ex.getDetails().code());
        assertEquals(422, ex.getDetails().httpStatus());
        server.verify();
    }

    @Test
    void refund_shouldCallRefundEndpoint() {
        server.expect(requestTo("http://localhost:8787/api/v1/refunds"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "idem-ref-1"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"refundId":"refund_123","status":"REFUNDED"}
                                """));

        BankRefundResponse response = client.refund(new BankRefundRequest("cap_123", 5000L, "USD"), "idem-ref-1");

        assertEquals("refund_123", response.refundId());
        assertEquals("REFUNDED", response.status());
        server.verify();
    }

    @Test
    void voidAuthorization_shouldCallVoidEndpoint() {
        server.expect(requestTo("http://localhost:8787/api/v1/voids"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "idem-void-1"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"voidId":"void_123","status":"VOIDED"}
                                """));

        BankVoidResponse response = client.voidAuthorization(new BankVoidRequest("auth_123"), "idem-void-1");

        assertEquals("void_123", response.voidId());
        assertEquals("VOIDED", response.status());
        server.verify();
    }

    @Test
    void authorize_shouldFailWhenResponseBodyIsEmpty() {
        server.expect(requestTo("http://localhost:8787/api/v1/authorizations"))
                .andRespond(withStatus(HttpStatus.OK));

        BankClientException ex = assertThrows(
                BankClientException.class,
                () -> client.authorize(
                        new BankAuthorizeRequest("4111111111111111", "123", "12", "2030", 5000L, "USD"),
                        "idem-auth-empty"));

        assertEquals("bank_empty_response", ex.getDetails().code());
        assertEquals(502, ex.getDetails().httpStatus());
    }

    @Test
    void constructor_shouldRejectInsecureNonLocalhostBaseUrl() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new BankHttpClient(
                        RestClient.builder(),
                        new BankErrorMapper(new ObjectMapper()),
                        "http://bank.example.com",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)));

        assertEquals("Insecure HTTP bank base URL is allowed only for localhost.", ex.getMessage());
    }
}
