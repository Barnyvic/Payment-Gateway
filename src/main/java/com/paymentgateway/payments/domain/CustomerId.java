package com.paymentgateway.payments.domain;

import java.util.Objects;

/** FicMart customer identifier. Immutable value object. */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("customerId must be non-blank");
        }
    }
}
