package com.paymentgateway.payments.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.Objects;
import java.util.UUID;

public record PaymentRef(UUID value) {

    public PaymentRef {
        Objects.requireNonNull(value, "value");
    }

    public static PaymentRef generate() {
        // UUIDv7 is time-ordered and improves index locality for write-heavy tables.
        return new PaymentRef(UuidCreator.getTimeOrderedEpoch());
    }
}
