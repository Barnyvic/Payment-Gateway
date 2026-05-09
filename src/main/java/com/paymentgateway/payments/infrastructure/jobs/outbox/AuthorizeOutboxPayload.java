package com.paymentgateway.payments.infrastructure.jobs.outbox;

public record AuthorizeOutboxPayload(
        String cardNumber, String cvv, String expiryMonth, String expiryYear, long amountCents, String currency) {}
