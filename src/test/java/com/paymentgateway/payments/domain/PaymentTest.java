package com.paymentgateway.payments.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentgateway.payments.domain.exception.InvalidPaymentTransitionException;
import com.paymentgateway.payments.domain.model.PaymentCommand;
import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void happyPath_authorizeCaptureRefund() {
        Payment p = newPayment();
        p.authorize();
        assertThat(p.getState()).isEqualTo(PaymentState.AUTHORIZED);
        p.capture();
        assertThat(p.getState()).isEqualTo(PaymentState.CAPTURED);
        p.refund();
        assertThat(p.getState()).isEqualTo(PaymentState.REFUNDED);
    }

    @Test
    void happyPath_authorizeVoid() {
        Payment p = newPayment();
        p.authorize();
        p.voidAuthorization();
        assertThat(p.getState()).isEqualTo(PaymentState.VOIDED);
    }

    @Test
    void cannotCaptureFromPending() {
        Payment p = newPayment();
        assertThatThrownBy(p::capture)
                .isInstanceOf(InvalidPaymentTransitionException.class)
                .satisfies(ex -> {
                    InvalidPaymentTransitionException e = (InvalidPaymentTransitionException) ex;
                    assertThat(e.getCurrentState()).isEqualTo(PaymentState.PENDING);
                    assertThat(e.getAttemptedCommand()).isEqualTo(PaymentCommand.CAPTURE);
                });
    }

    @Test
    void cannotVoidAfterCapture() {
        Payment p = newPayment();
        p.authorize();
        p.capture();
        assertThatThrownBy(p::voidAuthorization).isInstanceOf(InvalidPaymentTransitionException.class);
    }

    @Test
    void cannotCaptureAfterVoid() {
        Payment p = newPayment();
        p.authorize();
        p.voidAuthorization();
        assertThatThrownBy(p::capture).isInstanceOf(InvalidPaymentTransitionException.class);
    }

    @Test
    void cannotRefundFromAuthorized() {
        Payment p = newPayment();
        p.authorize();
        assertThatThrownBy(p::refund).isInstanceOf(InvalidPaymentTransitionException.class);
    }

    @Test
    void cannotDoubleAuthorize() {
        Payment p = newPayment();
        p.authorize();
        assertThatThrownBy(p::authorize).isInstanceOf(InvalidPaymentTransitionException.class);
    }

    @Test
    void terminalStatesRejectFurtherCommands() {
        Payment voided = newPayment();
        voided.authorize();
        voided.voidAuthorization();
        assertThatThrownBy(voided::refund).isInstanceOf(InvalidPaymentTransitionException.class);

        Payment refunded = newPayment();
        refunded.authorize();
        refunded.capture();
        refunded.refund();
        assertThatThrownBy(refunded::refund).isInstanceOf(InvalidPaymentTransitionException.class);
    }

    private static Payment newPayment() {
        return Payment.pending(
                new PaymentRef(UUID.randomUUID()),
                new OrderId("order-1"),
                new CustomerId("cust-1"),
                new Money(100L, SupportedCurrency.USD));
    }
}
