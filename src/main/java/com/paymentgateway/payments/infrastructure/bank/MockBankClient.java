package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
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
        try {
            return restClient
                    .post()
                    .uri("/api/v1/authorizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(BankAuthorizeResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = HttpStatusCode.valueOf(ex.getStatusCode().value());
            BankErrorDetails error = errorMapper.map(status, ex.getResponseBodyAsString());
            throw new BankClientException(error);
        }
    }
}
