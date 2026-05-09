package com.paymentgateway.payments.infrastructure.bank.model;

public record BankErrorDetails(
        String code, String message, int httpStatus, BankErrorCategory category, boolean retryable) {}
