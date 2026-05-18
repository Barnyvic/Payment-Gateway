package com.paymentgateway.payments.api;

public final class PaymentHttpConstants {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String X_IDEMPOTENT_REPLAYED = "X-Idempotent-Replayed";

    private PaymentHttpConstants() {}
}
