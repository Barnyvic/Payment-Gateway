package com.paymentgateway.payments.domain.value;

import java.util.Objects;

public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("customerId must be non-blank");
        }
    }
}
