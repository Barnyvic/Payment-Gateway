package com.paymentgateway.payments.infrastructure.persistence.entity;

import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.domain.outbox.model.OutboxStatus;
import com.paymentgateway.payments.infrastructure.persistence.converter.PaymentActionOutboxConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEventEntity {

    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "payment_ref", nullable = false)
    private UUID paymentRef;

    @Convert(converter = PaymentActionOutboxConverter.class)
    @Column(name = "event_type", nullable = false, length = 64)
    private PaymentAction eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (expiresAt == null) {
            expiresAt = Instant.now().plus(DEFAULT_TTL);
        }
        if (maxAttempts <= 0) {
            maxAttempts = 10;
        }
    }
}
