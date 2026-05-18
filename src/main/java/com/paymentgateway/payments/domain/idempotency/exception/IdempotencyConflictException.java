package com.paymentgateway.payments.domain.idempotency.exception;

import com.paymentgateway.common.util.PaymentAction;

public final class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(PaymentAction operation, String idempotencyKey) {
        super("Idempotency key conflict for operation " + operation + " and key " + idempotencyKey);
    }
}
