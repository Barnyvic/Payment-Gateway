package com.paymentgateway.payments.api.dto;

import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import java.util.List;

public record PaymentsByOrderResponse(String orderId, List<PaymentReceiptResponse> payments) {

    public static PaymentsByOrderResponse of(String orderId, List<PaymentReceiptRecord> records) {
        return new PaymentsByOrderResponse(
                orderId,
                records.stream().map(PaymentReceiptResponse::from).toList());
    }
}
