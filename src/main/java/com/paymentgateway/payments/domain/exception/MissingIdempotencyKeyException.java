package com.paymentgateway.payments.domain.exception;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required for this operation");
    }
}
