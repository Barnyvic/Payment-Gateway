package com.paymentgateway.payments.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentgateway.payments.domain.model.PaymentCommand;
import com.paymentgateway.payments.domain.model.PaymentState;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentStateTest {

    @Test
    void transitionTable_matchesSpec() {
        assertThat(PaymentState.PENDING.resolveTransition(PaymentCommand.AUTHORIZE))
                .isEqualTo(Optional.of(PaymentState.AUTHORIZED));
        assertThat(PaymentState.AUTHORIZED.resolveTransition(PaymentCommand.CAPTURE))
                .isEqualTo(Optional.of(PaymentState.CAPTURED));
        assertThat(PaymentState.AUTHORIZED.resolveTransition(PaymentCommand.VOID))
                .isEqualTo(Optional.of(PaymentState.VOIDED));
        assertThat(PaymentState.CAPTURED.resolveTransition(PaymentCommand.REFUND))
                .isEqualTo(Optional.of(PaymentState.REFUNDED));

        assertThat(PaymentState.PENDING.resolveTransition(PaymentCommand.CAPTURE)).isEmpty();
        assertThat(PaymentState.CAPTURED.resolveTransition(PaymentCommand.VOID)).isEmpty();
        assertThat(PaymentState.VOIDED.resolveTransition(PaymentCommand.CAPTURE)).isEmpty();
        assertThat(PaymentState.REFUNDED.resolveTransition(PaymentCommand.REFUND)).isEmpty();
    }
}
