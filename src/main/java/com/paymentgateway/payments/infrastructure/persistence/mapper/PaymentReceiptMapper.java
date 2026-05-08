package com.paymentgateway.payments.infrastructure.persistence.mapper;

import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.persistence.entity.PaymentReceiptEntity;
import java.time.Clock;
import java.time.Instant;

/**
 * Maps between the domain aggregate and the JPA receipt row. Bank columns are left unchanged on
 * merge unless explicitly cleared by future application services.
 */
public final class PaymentReceiptMapper {

    private PaymentReceiptMapper() {}

    public static Payment toDomain(PaymentReceiptEntity entity) {
        return Payment.rehydrate(
                new PaymentRef(entity.getPaymentRef()),
                new OrderId(entity.getOrderId()),
                new CustomerId(entity.getCustomerId()),
                new Money(entity.getAmountCents(), entity.getCurrency()),
                entity.getState());
    }

    public static void mergeAggregate(Payment payment, PaymentReceiptEntity entity, Clock clock) {
        entity.setPaymentRef(payment.getPaymentRef().value());
        entity.setOrderId(payment.getOrderId().value());
        entity.setCustomerId(payment.getCustomerId().value());
        entity.setAmountCents(payment.getMoney().amountMinorUnits());
        entity.setCurrency(payment.getMoney().currency());

        PaymentState newState = payment.getState();
        entity.setState(newState);

        Instant now = clock.instant();
        if (newState == PaymentState.AUTHORIZED && entity.getAuthorizedAt() == null) {
            entity.setAuthorizedAt(now);
        }
        if (newState == PaymentState.CAPTURED && entity.getCapturedAt() == null) {
            entity.setCapturedAt(now);
        }
        if (newState == PaymentState.VOIDED && entity.getVoidedAt() == null) {
            entity.setVoidedAt(now);
        }
        if (newState == PaymentState.REFUNDED && entity.getRefundedAt() == null) {
            entity.setRefundedAt(now);
        }
    }
}
