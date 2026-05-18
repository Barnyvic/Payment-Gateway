package com.paymentgateway.payments.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paymentgateway.AbstractPostgresIntegrationTest;
import com.paymentgateway.gateway.PaymentGatewayApplication;
import com.paymentgateway.payments.TestBankClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = PaymentGatewayApplication.class)
@AutoConfigureMockMvc
@Import(TestBankClientConfig.class)
@Transactional
class PaymentCommandsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authorize_returns202_thenReplay200WithHeader() throws Exception {
        String body =
                """
                {"orderId":"ord-cmd-1","customerId":"cust-cmd-1","card":{"number":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030"},"amountCents":100}
                """;

        mockMvc.perform(post("/v1/payments/authorize")
                        .header(PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, "idem-auth-a")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.paymentRef").exists())
                .andExpect(jsonPath("$.receiptStateAtEnqueue").value("AUTHORIZED"));

        mockMvc.perform(post("/v1/payments/authorize")
                        .header(PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, "idem-auth-a")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string(PaymentHttpConstants.X_IDEMPOTENT_REPLAYED, "true"))
                .andExpect(jsonPath("$.paymentRef").exists());
    }

    @Test
    void authorize_sameIdempotencyKeyDifferentBody_returns409() throws Exception {
        String body1 =
                """
                {"orderId":"ord-x","customerId":"cust-x","card":{"number":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030"},"amountCents":100}
                """;
        String body2 =
                """
                {"orderId":"ord-y","customerId":"cust-y","card":{"number":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030"},"amountCents":200}
                """;

        mockMvc.perform(post("/v1/payments/authorize")
                        .header(PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, "idem-conflict")
                        .contentType(APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/v1/payments/authorize")
                        .header(PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, "idem-conflict")
                        .contentType(APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void authorize_invalidBody_returns400() throws Exception {
        String body =
                """
                {"orderId":"","customerId":"cust-z","card":{"number":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030"},"amountCents":0}
                """;
        mockMvc.perform(post("/v1/payments/authorize")
                        .header(PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, "idem-invalid-body")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void authorize_blankIdempotencyKey_returns400() throws Exception {
        String body =
                """
                {"orderId":"ord-z","customerId":"cust-z","card":{"number":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030"},"amountCents":100}
                """;
        mockMvc.perform(post("/v1/payments/authorize")
                        .header(PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, "   ")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    void authorize_missingIdempotencyKey_returns400() throws Exception {
        String body =
                """
                {"orderId":"ord-z","customerId":"cust-z","card":{"number":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030"},"amountCents":100}
                """;
        mockMvc.perform(post("/v1/payments/authorize").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"));
    }
}
