package com.paymentgateway.payments.api.dto;

import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceiptResponse(
        UUID paymentRef,
        String orderId,
        String customerId,
        long amountCents,
        SupportedCurrency currency,
        PaymentState state,
        long version,
        Instant authorizedAt,
        Instant capturedAt,
        Instant voidedAt,
        Instant refundedAt,
        String bankAuthorizationId,
        String bankCaptureId,
        String bankVoidId,
        String bankRefundId,
        String lastErrorCode,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentReceiptResponse from(PaymentReceiptRecord record) {
        return new PaymentReceiptResponse(
                record.paymentRef(),
                record.orderId(),
                record.customerId(),
                record.amountCents(),
                record.currency(),
                record.state(),
                record.version(),
                record.authorizedAt(),
                record.capturedAt(),
                record.voidedAt(),
                record.refundedAt(),
                record.bankAuthorizationId(),
                record.bankCaptureId(),
                record.bankVoidId(),
                record.bankRefundId(),
                record.lastErrorCode(),
                record.lastErrorMessage(),
                record.createdAt(),
                record.updatedAt());
    }
}
