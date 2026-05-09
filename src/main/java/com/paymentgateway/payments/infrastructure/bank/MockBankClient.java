package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorDetails;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MockBankClient implements BankClient {
    private final RestClient restClient;
    private final BankErrorMapper errorMapper;

    public MockBankClient(
            RestClient.Builder restClientBuilder,
            BankErrorMapper errorMapper,
            @Value("${payments.bank.base-url:http://localhost:8787}") String bankBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(bankBaseUrl).build();
        this.errorMapper = errorMapper;
    }

    @Override
    public BankAuthorizeResponse authorize(BankAuthorizeRequest request, String idempotencyKey) {
        return doPost("/api/v1/authorizations", request, idempotencyKey, BankAuthorizeResponse.class);
    }

    @Override
    public BankCaptureResponse capture(BankCaptureRequest request, String idempotencyKey) {
        return doPost("/api/v1/captures", request, idempotencyKey, BankCaptureResponse.class);
    }

    @Override
    public BankVoidResponse voidAuthorization(BankVoidRequest request, String idempotencyKey) {
        return doPost("/api/v1/voids", request, idempotencyKey, BankVoidResponse.class);
    }

    @Override
    public BankRefundResponse refund(BankRefundRequest request, String idempotencyKey) {
        return doPost("/api/v1/refunds", request, idempotencyKey, BankRefundResponse.class);
    }

    private <T> T doPost(String path, Object requestBody, String idempotencyKey, Class<T> responseType) {
        try {
            return restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(requestBody)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = HttpStatusCode.valueOf(ex.getStatusCode().value());
            BankErrorDetails error = errorMapper.map(status, ex.getResponseBodyAsString());
            throw new BankClientException(error);
        } catch (RestClientException ex) {
            BankErrorDetails error = errorMapper.map(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
            throw new BankClientException(error);
        }
    }
}
