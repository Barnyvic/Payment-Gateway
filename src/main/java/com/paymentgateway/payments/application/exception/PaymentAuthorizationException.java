package com.paymentgateway.payments.application.exception;

public class PaymentAuthorizationException extends RuntimeException {

    private final String errorCode;
    private final boolean transientFailure;

    public PaymentAuthorizationException(String errorCode, String message, boolean transientFailure) {
        super(message);
        this.errorCode = errorCode;
        this.transientFailure = transientFailure;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isTransientFailure() {
        return transientFailure;
    }
}
