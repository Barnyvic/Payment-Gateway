package com.paymentgateway.payments.domain.outbox.model;

public enum OutboxEventType {
    PAYMENT_AUTHORIZE_REQUESTED,
    PAYMENT_CAPTURE_REQUESTED,
    PAYMENT_VOID_REQUESTED,
    PAYMENT_REFUND_REQUESTED
}
