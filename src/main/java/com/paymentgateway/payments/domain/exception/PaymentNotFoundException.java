package com.paymentgateway.payments.domain.exception;

import com.paymentgateway.payments.domain.value.PaymentRef;

public class PaymentNotFoundException extends RuntimeException {

    private final PaymentRef paymentRef;

    public PaymentNotFoundException(PaymentRef paymentRef) {
        super("Payment not found: " + paymentRef.value());
        this.paymentRef = paymentRef;
    }

    public PaymentRef getPaymentRef() {
        return paymentRef;
    }
}
