package com.paymentgateway.payments.domain.value;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.Objects;
import java.util.UUID;

public record PaymentRef(UUID value) {

    public PaymentRef {
        Objects.requireNonNull(value, "value");
    }

    public static PaymentRef generate() {
        return new PaymentRef(UuidCreator.getTimeOrderedEpoch());
    }
}
