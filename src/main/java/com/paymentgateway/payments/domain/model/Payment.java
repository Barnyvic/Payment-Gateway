package com.paymentgateway.payments.domain.model;

import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.domain.exception.InvalidPaymentTransitionException;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
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
        transition(PaymentAction.AUTHORIZE);
    }

    public void capture() {
        transition(PaymentAction.CAPTURE);
    }

    public void voidAuthorization() {
        transition(PaymentAction.VOID);
    }

    public void refund() {
        transition(PaymentAction.REFUND);
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

    private void transition(PaymentAction action) {
        PaymentState current = this.state;
        this.state = current
                .resolveTransition(action)
                .orElseThrow(() -> new InvalidPaymentTransitionException(current, action));
    }
}
