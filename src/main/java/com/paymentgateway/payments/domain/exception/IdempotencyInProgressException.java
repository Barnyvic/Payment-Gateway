package com.paymentgateway.payments.domain.exception;

import com.paymentgateway.common.util.PaymentAction;

public class IdempotencyInProgressException extends RuntimeException {

    private final PaymentAction operation;
    private final String idempotencyKey;

    public IdempotencyInProgressException(PaymentAction operation, String idempotencyKey) {
        super("Idempotency record still in progress for operation " + operation + " and key " + idempotencyKey);
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
    }

    public PaymentAction getOperation() {
        return operation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
