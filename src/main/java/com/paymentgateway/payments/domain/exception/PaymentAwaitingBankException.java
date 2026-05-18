package com.paymentgateway.payments.domain.exception;

public class PaymentAwaitingBankException extends RuntimeException {

    public PaymentAwaitingBankException(String message) {
        super(message);
    }
}
