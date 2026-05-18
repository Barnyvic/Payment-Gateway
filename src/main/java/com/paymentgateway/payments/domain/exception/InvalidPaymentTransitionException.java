package com.paymentgateway.payments.domain.exception;

import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.domain.model.PaymentState;

public final class InvalidPaymentTransitionException extends RuntimeException {

    private final PaymentState currentState;
    private final PaymentAction attemptedAction;

    public InvalidPaymentTransitionException(PaymentState currentState, PaymentAction attemptedAction) {
        super("Cannot execute " + attemptedAction + " in state " + currentState);
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }

    public PaymentState getCurrentState() {
        return currentState;
    }

    public PaymentAction getAttemptedAction() {
        return attemptedAction;
    }
}
