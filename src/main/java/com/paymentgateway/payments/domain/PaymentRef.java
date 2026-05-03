package com.paymentgateway.payments.domain;

import java.util.Objects;
import java.util.UUID;

public record PaymentRef(UUID value) {

    public PaymentRef {
        Objects.requireNonNull(value, "value");
    }

    public static PaymentRef generate() {
        return new PaymentRef(UUID.randomUUID());
    }
}
