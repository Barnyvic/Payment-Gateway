package com.paymentgateway.payments.domain;

public final class InvalidPaymentTransitionException extends RuntimeException {

    private final PaymentState currentState;
    private final PaymentCommand attemptedCommand;

    public InvalidPaymentTransitionException(PaymentState currentState, PaymentCommand attemptedCommand) {
        super("Cannot execute " + attemptedCommand + " in state " + currentState);
        this.currentState = currentState;
        this.attemptedCommand = attemptedCommand;
    }

    public PaymentState getCurrentState() {
        return currentState;
    }

    public PaymentCommand getAttemptedCommand() {
        return attemptedCommand;
    }
}
