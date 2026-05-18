package com.paymentgateway.payments.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.domain.model.PaymentState;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentStateTest {

    @Test
    void transitionTable_matchesSpec() {
        assertThat(PaymentState.PENDING.resolveTransition(PaymentAction.AUTHORIZE))
                .isEqualTo(Optional.of(PaymentState.AUTHORIZED));
        assertThat(PaymentState.AUTHORIZED.resolveTransition(PaymentAction.CAPTURE))
                .isEqualTo(Optional.of(PaymentState.CAPTURED));
        assertThat(PaymentState.AUTHORIZED.resolveTransition(PaymentAction.VOID))
                .isEqualTo(Optional.of(PaymentState.VOIDED));
        assertThat(PaymentState.CAPTURED.resolveTransition(PaymentAction.REFUND))
                .isEqualTo(Optional.of(PaymentState.REFUNDED));

        assertThat(PaymentState.PENDING.resolveTransition(PaymentAction.CAPTURE)).isEmpty();
        assertThat(PaymentState.CAPTURED.resolveTransition(PaymentAction.VOID)).isEmpty();
        assertThat(PaymentState.VOIDED.resolveTransition(PaymentAction.CAPTURE)).isEmpty();
        assertThat(PaymentState.REFUNDED.resolveTransition(PaymentAction.REFUND)).isEmpty();
    }
}
