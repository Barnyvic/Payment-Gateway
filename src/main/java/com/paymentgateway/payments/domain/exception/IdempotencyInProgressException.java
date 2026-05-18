package com.paymentgateway.payments.domain.exception;

import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;

public class IdempotencyInProgressException extends RuntimeException {

    private final PaymentOperation operation;
    private final String idempotencyKey;

    public IdempotencyInProgressException(PaymentOperation operation, String idempotencyKey) {
        super("Idempotency record still in progress for operation " + operation + " and key " + idempotencyKey);
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
    }

    public PaymentOperation getOperation() {
        return operation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
