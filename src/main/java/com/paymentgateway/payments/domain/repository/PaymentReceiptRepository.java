package com.paymentgateway.payments.domain.repository;

import com.paymentgateway.payments.domain.CustomerId;
import com.paymentgateway.payments.domain.OrderId;
import com.paymentgateway.payments.domain.Payment;
import com.paymentgateway.payments.domain.PaymentRef;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persist and load payment receipts (gateway source of truth). Implementations
 * live in infrastructure; the domain aggregate stays persistence-agnostic.
 */
public interface PaymentReceiptRepository {

    Payment save(Payment payment);

    Optional<Payment> findByPaymentRef(PaymentRef paymentRef);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(OrderId orderId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(CustomerId customerId);
}
