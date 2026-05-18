package com.paymentgateway.payments.domain.repository;

import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import java.util.List;
import java.util.Optional;

public interface PaymentReceiptRepository {

    Payment save(Payment payment);

    Optional<Payment> findByPaymentRef(PaymentRef paymentRef);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(OrderId orderId);

    List<PaymentReceiptRecord> findReceiptRecordsByOrderId(OrderId orderId);

    List<PaymentReceiptRecord> findReceiptRecordsByCustomerId(CustomerId customerId, int limit);

    Optional<PaymentReceiptRecord> findReceiptRecordByPaymentRef(PaymentRef paymentRef);

    void recordAuthorizeSuccess(PaymentRef paymentRef, String bankAuthorizationId);

    void recordCaptureSuccess(PaymentRef paymentRef, String bankCaptureId);

    void recordVoidSuccess(PaymentRef paymentRef, String bankVoidId);

    void recordRefundSuccess(PaymentRef paymentRef, String bankRefundId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(CustomerId customerId);
}
