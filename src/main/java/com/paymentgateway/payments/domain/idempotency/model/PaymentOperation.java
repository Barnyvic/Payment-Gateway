package com.paymentgateway.payments.domain.idempotency.model;

public enum PaymentOperation {
    AUTHORIZE,
    CAPTURE,
    VOID,
    REFUND
}
