package com.paymentgateway.payments.domain.idempotency.exception;

import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;

public final class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(PaymentOperation operation, String idempotencyKey) {
        super("Idempotency key conflict for operation " + operation + " and key " + idempotencyKey);
    }
}
