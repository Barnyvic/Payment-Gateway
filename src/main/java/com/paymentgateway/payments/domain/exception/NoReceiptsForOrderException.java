package com.paymentgateway.payments.domain.exception;

import com.paymentgateway.payments.domain.value.OrderId;

public class NoReceiptsForOrderException extends RuntimeException {

    private final OrderId orderId;

    public NoReceiptsForOrderException(OrderId orderId) {
        super("No payment receipts for order: " + orderId.value());
        this.orderId = orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
