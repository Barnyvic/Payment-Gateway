package com.paymentgateway.payments.infrastructure.bank.model;

public record BankRefundRequest(String captureId, long amountCents, String currency) {}
