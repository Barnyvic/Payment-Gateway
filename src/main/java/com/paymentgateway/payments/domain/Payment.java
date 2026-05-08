package com.paymentgateway.payments.domain;

import java.util.Objects;

public final class Payment {

    private final PaymentRef paymentRef;
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money money;
    private PaymentState state;

    private Payment(
            PaymentRef paymentRef,
            OrderId orderId,
            CustomerId customerId,
            Money money,
            PaymentState state) {
        this.paymentRef = Objects.requireNonNull(paymentRef, "paymentRef");
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.money = Objects.requireNonNull(money, "money");
        this.state = Objects.requireNonNull(state, "state");
    }


    public static Payment pending(PaymentRef paymentRef, OrderId orderId, CustomerId customerId, Money money) {
        return new Payment(paymentRef, orderId, customerId, money, PaymentState.PENDING);
    }

    /**
     * Reconstructs the aggregate from stored state. Used by the persistence layer only; callers must
     * not use this to skip lifecycle rules on new payments.
     */
    public static Payment rehydrate(
            PaymentRef paymentRef,
            OrderId orderId,
            CustomerId customerId,
            Money money,
            PaymentState state) {
        Objects.requireNonNull(state, "state");
        return new Payment(paymentRef, orderId, customerId, money, state);
    }

    public void authorize() {
        transition(PaymentCommand.AUTHORIZE);
    }

    public void capture() {
        transition(PaymentCommand.CAPTURE);
    }

    public void voidAuthorization() {
        transition(PaymentCommand.VOID);
    }

    public void refund() {
        transition(PaymentCommand.REFUND);
    }

    public PaymentRef getPaymentRef() {
        return paymentRef;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public Money getMoney() {
        return money;
    }

    public PaymentState getState() {
        return state;
    }

    private void transition(PaymentCommand command) {
        PaymentState current = this.state;
        this.state = current
                .resolveTransition(command)
                .orElseThrow(() -> new InvalidPaymentTransitionException(current, command));
    }
}
