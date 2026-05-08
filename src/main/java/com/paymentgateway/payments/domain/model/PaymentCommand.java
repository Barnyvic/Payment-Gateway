package com.paymentgateway.payments.domain.model;

public enum PaymentCommand {
    AUTHORIZE,
    CAPTURE,
    VOID,
    REFUND
}
