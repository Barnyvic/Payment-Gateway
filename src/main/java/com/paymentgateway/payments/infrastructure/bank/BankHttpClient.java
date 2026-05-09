package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorDetails;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BankHttpClient implements BankClient {
    private final RestClient restClient;
    private final BankErrorMapper errorMapper;

    public BankHttpClient(
            RestClient.Builder restClientBuilder,
            BankErrorMapper errorMapper,
            @Value("${payments.bank.base-url:https://localhost:8787}") String bankBaseUrl,
            @Value("${payments.bank.connect-timeout:2s}") Duration connectTimeout,
            @Value("${payments.bank.read-timeout:5s}") Duration readTimeout) {
        this(buildRestClient(restClientBuilder, bankBaseUrl, connectTimeout, readTimeout), errorMapper);
    }

    BankHttpClient(RestClient restClient, BankErrorMapper errorMapper) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
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
            T response = restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(requestBody)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw new BankClientException(new BankErrorDetails(
                        "bank_empty_response",
                        "Bank returned empty response body.",
                        502,
                        BankErrorCategory.TRANSIENT,
                        true));
            }
            return response;
        } catch (RestClientResponseException ex) {
            BankErrorDetails error = errorMapper.map(ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BankClientException(error);
        } catch (ResourceAccessException ex) {
            throw new BankClientException(classifyResourceAccessException(ex));
        }
    }

    private static RestClient buildRestClient(
            RestClient.Builder restClientBuilder,
            String bankBaseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        validateBankBaseUrl(bankBaseUrl);
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Objects.requireNonNull(connectTimeout, "connectTimeout")).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Objects.requireNonNull(readTimeout, "readTimeout"));
        return restClientBuilder.requestFactory(requestFactory).baseUrl(bankBaseUrl).build();
    }

    private static BankErrorDetails classifyResourceAccessException(ResourceAccessException ex) {
        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause instanceof HttpTimeoutException || rootCause instanceof java.net.SocketTimeoutException) {
            return new BankErrorDetails(
                    "bank_timeout", "Bank request timed out.", 504, BankErrorCategory.TRANSIENT, true);
        }
        if (rootCause instanceof ConnectException) {
            return new BankErrorDetails(
                    "bank_connection_refused", "Bank connection was refused.", 503, BankErrorCategory.TRANSIENT, true);
        }
        return new BankErrorDetails(
                "bank_io_error", "Bank endpoint is unreachable.", 503, BankErrorCategory.TRANSIENT, true);
    }

    private static void validateBankBaseUrl(String bankBaseUrl) {
        URI uri = URI.create(bankBaseUrl);
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("payments.bank.base-url must include a URL scheme.");
        }
        if ("http".equalsIgnoreCase(scheme) && !isLocalhost(uri.getHost())) {
            throw new IllegalArgumentException("Insecure HTTP bank base URL is allowed only for localhost.");
        }
    }

    private static boolean isLocalhost(String host) {
        if (host == null) {
            return false;
        }
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
