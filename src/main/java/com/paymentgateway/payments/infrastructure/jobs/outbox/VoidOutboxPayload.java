package com.paymentgateway.payments.infrastructure.jobs.outbox;

public record VoidOutboxPayload(String authorizationId) {}
