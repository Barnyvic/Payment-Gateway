package com.paymentgateway.payments.domain;

import java.util.Objects;

public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("orderId must be non-blank");
        }
    }
}
