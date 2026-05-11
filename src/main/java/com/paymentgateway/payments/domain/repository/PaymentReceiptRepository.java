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

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(CustomerId customerId);
}
