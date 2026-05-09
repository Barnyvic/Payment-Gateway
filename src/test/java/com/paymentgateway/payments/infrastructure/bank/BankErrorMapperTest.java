package com.paymentgateway.payments.infrastructure.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BankErrorMapperTest {

    private final BankErrorMapper mapper = new BankErrorMapper(new ObjectMapper());

    @Test
    void map_shouldClassify5xxAsTransient() {
        String body = """
                {
                  "error_code": "bank_unavailable",
                  "message": "Temporary outage"
                }
                """;

        BankErrorDetails details = mapper.map(HttpStatus.INTERNAL_SERVER_ERROR, body);

        assertEquals("bank_unavailable", details.code());
        assertEquals("Temporary outage", details.message());
        assertEquals(BankErrorCategory.TRANSIENT, details.category());
        assertTrue(details.retryable());
    }

    @Test
    void map_shouldClassify4xxAsPermanent() {
        String body = """
                {
                  "code": "insufficient_funds",
                  "message": "Insufficient funds"
                }
                """;

        BankErrorDetails details = mapper.map(HttpStatus.UNPROCESSABLE_ENTITY, body);

        assertEquals("insufficient_funds", details.code());
        assertEquals("Insufficient funds", details.message());
        assertEquals(BankErrorCategory.PERMANENT, details.category());
        assertEquals(false, details.retryable());
    }
}
