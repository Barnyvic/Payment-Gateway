package com.paymentgateway.payments.domain.idempotency.model;

public enum IdempotencyStatus {
    IN_PROGRESS,
    /** Async command persisted (outbox); response snapshot is safe to replay. */
    ACCEPTED,
    SUCCEEDED,
    FAILED
}
