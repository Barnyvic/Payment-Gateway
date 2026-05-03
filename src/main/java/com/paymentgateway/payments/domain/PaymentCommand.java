package com.paymentgateway.payments.domain;

public enum PaymentCommand {
    AUTHORIZE,
    CAPTURE,
    VOID,
    REFUND
}
