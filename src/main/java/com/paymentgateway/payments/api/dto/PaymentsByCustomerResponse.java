package com.paymentgateway.payments.api.dto;

import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import java.util.List;

public record PaymentsByCustomerResponse(String customerId, List<PaymentReceiptResponse> payments) {

    public static PaymentsByCustomerResponse of(String customerId, List<PaymentReceiptRecord> records) {
        return new PaymentsByCustomerResponse(
                customerId,
                records.stream().map(PaymentReceiptResponse::from).toList());
    }
}
