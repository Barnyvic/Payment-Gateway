package com.paymentgateway.payments.domain.value;

import java.util.Objects;

public record Money(long amountMinorUnits, SupportedCurrency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (amountMinorUnits <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
