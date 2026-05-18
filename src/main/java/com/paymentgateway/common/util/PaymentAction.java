package com.paymentgateway.common.util;

import java.util.Arrays;

public enum PaymentAction {
    AUTHORIZE,
    CAPTURE,
    VOID,
    REFUND;

    public String outboxEventTypeName() {
        return "PAYMENT_" + name() + "_REQUESTED";
    }

    public String bankIdempotencySegment() {
        return outboxEventTypeName().toLowerCase();
    }

    public static PaymentAction fromOutboxEventTypeName(String name) {
        return Arrays.stream(values())
                .filter(action -> action.outboxEventTypeName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown outbox event type: " + name));
    }
}
