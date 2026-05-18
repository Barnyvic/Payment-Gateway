package com.paymentgateway.payments.infrastructure.persistence.mapper;

import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.persistence.entity.OutboxEventEntity;

public final class OutboxEventMapper {

    private OutboxEventMapper() {}

    public static OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.rehydrate(
                entity.getEventId(),
                new PaymentRef(entity.getPaymentRef()),
                entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getNextAttemptAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static void merge(OutboxEvent outboxEvent, OutboxEventEntity entity) {
        entity.setEventId(outboxEvent.getEventId());
        entity.setPaymentRef(outboxEvent.getPaymentRef().value());
        entity.setEventType(outboxEvent.getAction());
        entity.setPayload(outboxEvent.getPayload());
        entity.setStatus(outboxEvent.getStatus());
        entity.setAttemptCount(outboxEvent.getAttemptCount());
        entity.setNextAttemptAt(outboxEvent.getNextAttemptAt());
    }
}
