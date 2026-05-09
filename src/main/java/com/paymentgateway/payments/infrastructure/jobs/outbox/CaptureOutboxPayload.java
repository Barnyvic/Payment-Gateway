package com.paymentgateway.payments.infrastructure.jobs.outbox;

public record CaptureOutboxPayload(String authorizationId, long amountCents, String currency) {}
