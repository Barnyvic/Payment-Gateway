package com.paymentgateway.payments.domain.outbox.model;

import com.paymentgateway.payments.domain.value.PaymentRef;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OutboxEvent {

    private final UUID eventId;
    private final PaymentRef paymentRef;
    private final OutboxEventType eventType;
    private final String payload;
    private OutboxStatus status;
    private int attemptCount;
    private Instant nextAttemptAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private OutboxEvent(
            UUID eventId,
            PaymentRef paymentRef,
            OutboxEventType eventType,
            String payload,
            OutboxStatus status,
            int attemptCount,
            Instant nextAttemptAt,
            Instant createdAt,
            Instant updatedAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.paymentRef = Objects.requireNonNull(paymentRef, "paymentRef");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.payload = requireNonBlank(payload, "payload");
        this.status = Objects.requireNonNull(status, "status");
        this.attemptCount = attemptCount;
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static OutboxEvent enqueue(
            UUID eventId, PaymentRef paymentRef, OutboxEventType eventType, String payload, Instant now) {
        Instant ts = Objects.requireNonNull(now, "now");
        return new OutboxEvent(eventId, paymentRef, eventType, payload, OutboxStatus.PENDING, 0, ts, ts, ts);
    }

    public static OutboxEvent rehydrate(
            UUID eventId,
            PaymentRef paymentRef,
            OutboxEventType eventType,
            String payload,
            OutboxStatus status,
            int attemptCount,
            Instant nextAttemptAt,
            Instant createdAt,
            Instant updatedAt) {
        return new OutboxEvent(
                eventId, paymentRef, eventType, payload, status, attemptCount, nextAttemptAt, createdAt, updatedAt);
    }

    public UUID getEventId() {
        return eventId;
    }

    public PaymentRef getPaymentRef() {
        return paymentRef;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be non-blank");
        }
        return value;
    }
}
