package com.paymentgateway.payments.infrastructure.bank.model;

public record BankAuthorizeRequest(
        String cardNumber, String cvv, String expiryMonth, String expiryYear, long amountCents, String currency) {}
