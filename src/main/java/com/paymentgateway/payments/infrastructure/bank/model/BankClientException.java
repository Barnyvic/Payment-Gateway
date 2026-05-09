package com.paymentgateway.payments.infrastructure.bank.model;

public class BankClientException extends RuntimeException {
    private final BankErrorDetails details;

    public BankClientException(BankErrorDetails details) {
        super(details.message());
        this.details = details;
    }

    public BankErrorDetails getDetails() {
        return details;
    }
}
