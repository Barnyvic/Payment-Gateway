package com.paymentgateway.payments.domain.query;

import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import java.time.Instant;
import java.util.UUID;


public record PaymentReceiptRecord(
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
        Instant updatedAt) {}
