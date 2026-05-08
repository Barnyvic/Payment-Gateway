package com.paymentgateway.payments.domain.idempotency.model;

import com.paymentgateway.payments.domain.idempotency.exception.IdempotencyConflictException;
import com.paymentgateway.payments.domain.value.PaymentRef;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class IdempotencyRecord {

    private final UUID id;
    private final PaymentOperation operation;
    private final String idempotencyKey;
    private final String requestHash;
    private IdempotencyStatus status;
    private String responseSnapshot;
    private PaymentRef paymentRef;
    private final Instant createdAt;
    private Instant updatedAt;

    private IdempotencyRecord(
            UUID id,
            PaymentOperation operation,
            String idempotencyKey,
            String requestHash,
            IdempotencyStatus status,
            String responseSnapshot,
            PaymentRef paymentRef,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.idempotencyKey = requireNonBlank(idempotencyKey, "idempotencyKey");
        this.requestHash = requireNonBlank(requestHash, "requestHash");
        this.status = Objects.requireNonNull(status, "status");
        this.responseSnapshot = responseSnapshot;
        this.paymentRef = paymentRef;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        validateInvariants();
    }

    public static IdempotencyRecord start(
            UUID id, PaymentOperation operation, String idempotencyKey, String requestHash, Instant now) {
        Instant ts = Objects.requireNonNull(now, "now");
        return new IdempotencyRecord(
                id, operation, idempotencyKey, requestHash, IdempotencyStatus.IN_PROGRESS, null, null, ts, ts);
    }

    public static IdempotencyRecord rehydrate(
            UUID id,
            PaymentOperation operation,
            String idempotencyKey,
            String requestHash,
            IdempotencyStatus status,
            String responseSnapshot,
            PaymentRef paymentRef,
            Instant createdAt,
            Instant updatedAt) {
        return new IdempotencyRecord(
                id, operation, idempotencyKey, requestHash, status, responseSnapshot, paymentRef, createdAt, updatedAt);
    }

    public void ensureRequestHashMatches(String candidateHash) {
        if (!requestHash.equals(requireNonBlank(candidateHash, "candidateHash"))) {
            throw new IdempotencyConflictException(operation, idempotencyKey);
        }
    }

    public void markSucceeded(String responseSnapshot, PaymentRef paymentRef, Instant now) {
        requireInProgressTransition("markSucceeded");
        this.status = IdempotencyStatus.SUCCEEDED;
        this.responseSnapshot = requireNonBlank(responseSnapshot, "responseSnapshot");
        this.paymentRef = Objects.requireNonNull(paymentRef, "paymentRef");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void markFailed(String responseSnapshot, Instant now) {
        requireInProgressTransition("markFailed");
        this.status = IdempotencyStatus.FAILED;
        this.responseSnapshot = requireNonBlank(responseSnapshot, "responseSnapshot");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public UUID getId() {
        return id;
    }

    public PaymentOperation getOperation() {
        return operation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Optional<String> getResponseSnapshot() {
        return Optional.ofNullable(responseSnapshot);
    }

    public Optional<PaymentRef> getPaymentRef() {
        return Optional.ofNullable(paymentRef);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IdempotencyRecord other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "IdempotencyRecord{id="
                + id
                + ", operation="
                + operation
                + ", idempotencyKey='"
                + idempotencyKey
                + "', status="
                + status
                + ", createdAt="
                + createdAt
                + ", updatedAt="
                + updatedAt
                + "}";
    }

    private void requireInProgressTransition(String action) {
        if (status != IdempotencyStatus.IN_PROGRESS) {
            throw new IllegalStateException(action + " requires IN_PROGRESS state but was " + status);
        }
    }

    private void validateInvariants() {
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must be >= createdAt");
        }
        if (status == IdempotencyStatus.IN_PROGRESS) {
            if (responseSnapshot != null || paymentRef != null) {
                throw new IllegalArgumentException("IN_PROGRESS must not carry responseSnapshot or paymentRef");
            }
        }
        if (status == IdempotencyStatus.SUCCEEDED) {
            if (responseSnapshot == null || responseSnapshot.isBlank()) {
                throw new IllegalArgumentException("SUCCEEDED requires non-blank responseSnapshot");
            }
            if (paymentRef == null) {
                throw new IllegalArgumentException("SUCCEEDED requires paymentRef");
            }
        }
        if (status == IdempotencyStatus.FAILED) {
            if (responseSnapshot == null || responseSnapshot.isBlank()) {
                throw new IllegalArgumentException("FAILED requires non-blank responseSnapshot");
            }
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be non-blank");
        }
        return value;
    }
}
