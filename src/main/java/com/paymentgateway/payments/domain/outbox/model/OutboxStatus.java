package com.paymentgateway.payments.domain.outbox.model;

public enum OutboxStatus {
    PENDING,
    IN_PROGRESS,
    PROCESSED,
    FAILED
}
