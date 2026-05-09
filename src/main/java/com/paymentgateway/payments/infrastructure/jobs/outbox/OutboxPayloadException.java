package com.paymentgateway.payments.infrastructure.jobs.outbox;

public class OutboxPayloadException extends RuntimeException {
    public OutboxPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
