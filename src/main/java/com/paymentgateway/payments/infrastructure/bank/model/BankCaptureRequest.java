package com.paymentgateway.payments.infrastructure.bank.model;

public record BankCaptureRequest(String authorizationId, long amountCents, String currency) {}
