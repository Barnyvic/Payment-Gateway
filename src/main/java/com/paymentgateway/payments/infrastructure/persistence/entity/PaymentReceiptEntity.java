package com.paymentgateway.payments.infrastructure.persistence.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_receipts")
@Getter
@Setter
@NoArgsConstructor
public class PaymentReceiptEntity {

    @Id
    @Column(name = "payment_ref", nullable = false, updatable = false)
    private UUID paymentRef;

    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private SupportedCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private PaymentState state;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "bank_authorization_id", length = 255)
    private String bankAuthorizationId;

    @Column(name = "bank_capture_id", length = 255)
    private String bankCaptureId;

    @Column(name = "bank_void_id", length = 255)
    private String bankVoidId;

    @Column(name = "bank_refund_id", length = 255)
    private String bankRefundId;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentReceiptEntity() {
        this.paymentRef = UuidCreator.getTimeOrderedEpoch();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
