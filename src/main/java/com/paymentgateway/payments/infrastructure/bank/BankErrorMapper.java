package com.paymentgateway.payments.infrastructure.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorDetails;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
public class BankErrorMapper {
    private final ObjectMapper objectMapper;

    public BankErrorMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BankErrorDetails map(HttpStatusCode statusCode, String responseBody) {
        int status = statusCode.value();
        String code = "bank_error";
        String message = "Bank request failed.";

        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.hasNonNull("error_code")) {
                    code = root.get("error_code").asText(code);
                } else if (root.hasNonNull("code")) {
                    code = root.get("code").asText(code);
                }

                if (root.hasNonNull("message")) {
                    message = root.get("message").asText(message);
                } else if (root.hasNonNull("error_message")) {
                    message = root.get("error_message").asText(message);
                }
            } catch (Exception ignored) {
            }
        }

        boolean transientFailure = status >= 500 || status == 408 || status == 429;
        return new BankErrorDetails(
                code,
                message,
                status,
                transientFailure ? BankErrorCategory.TRANSIENT : BankErrorCategory.PERMANENT,
                transientFailure);
    }
}
