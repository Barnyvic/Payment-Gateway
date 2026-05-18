package com.paymentgateway.payments.domain.model;

import com.paymentgateway.common.util.PaymentAction;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public enum PaymentState {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    VOIDED;

    private static final Map<PaymentState, Map<PaymentAction, PaymentState>> TRANSITIONS = buildTransitionTable();

    public Optional<PaymentState> resolveTransition(PaymentAction action) {
        Map<PaymentAction, PaymentState> row = TRANSITIONS.get(this);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(row.get(action));
    }

    private static Map<PaymentState, Map<PaymentAction, PaymentState>> buildTransitionTable() {
        Map<PaymentState, Map<PaymentAction, PaymentState>> table = new EnumMap<>(PaymentState.class);

        Map<PaymentAction, PaymentState> pending = new EnumMap<>(PaymentAction.class);
        pending.put(PaymentAction.AUTHORIZE, AUTHORIZED);
        table.put(PENDING, pending);

        Map<PaymentAction, PaymentState> authorized = new EnumMap<>(PaymentAction.class);
        authorized.put(PaymentAction.CAPTURE, CAPTURED);
        authorized.put(PaymentAction.VOID, VOIDED);
        table.put(AUTHORIZED, authorized);

        Map<PaymentAction, PaymentState> captured = new EnumMap<>(PaymentAction.class);
        captured.put(PaymentAction.REFUND, REFUNDED);
        table.put(CAPTURED, captured);

        table.put(REFUNDED, new EnumMap<>(PaymentAction.class));
        table.put(VOIDED, new EnumMap<>(PaymentAction.class));

        return Map.copyOf(table);
    }
}
