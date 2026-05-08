package com.paymentgateway.payments.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public enum PaymentState {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    VOIDED;

    private static final Map<PaymentState, Map<PaymentCommand, PaymentState>> TRANSITIONS = buildTransitionTable();

    public Optional<PaymentState> resolveTransition(PaymentCommand command) {
        Map<PaymentCommand, PaymentState> row = TRANSITIONS.get(this);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(row.get(command));
    }

    private static Map<PaymentState, Map<PaymentCommand, PaymentState>> buildTransitionTable() {
        Map<PaymentState, Map<PaymentCommand, PaymentState>> table = new EnumMap<>(PaymentState.class);

        Map<PaymentCommand, PaymentState> pending = new EnumMap<>(PaymentCommand.class);
        pending.put(PaymentCommand.AUTHORIZE, AUTHORIZED);
        table.put(PENDING, pending);

        Map<PaymentCommand, PaymentState> authorized = new EnumMap<>(PaymentCommand.class);
        authorized.put(PaymentCommand.CAPTURE, CAPTURED);
        authorized.put(PaymentCommand.VOID, VOIDED);
        table.put(AUTHORIZED, authorized);

        Map<PaymentCommand, PaymentState> captured = new EnumMap<>(PaymentCommand.class);
        captured.put(PaymentCommand.REFUND, REFUNDED);
        table.put(CAPTURED, captured);

        table.put(REFUNDED, new EnumMap<>(PaymentCommand.class));
        table.put(VOIDED, new EnumMap<>(PaymentCommand.class));

        return Map.copyOf(table);
    }
}
