package com.paymentgateway.payments.domain.idempotency.model;

public enum IdempotencyStatus {
    IN_PROGRESS,
    ACCEPTED,
    SUCCEEDED,
    FAILED
}
