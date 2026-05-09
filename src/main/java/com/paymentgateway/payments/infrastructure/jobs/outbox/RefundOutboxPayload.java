package com.paymentgateway.payments.infrastructure.jobs.outbox;

public record RefundOutboxPayload(String captureId, long amountCents, String currency) {}
