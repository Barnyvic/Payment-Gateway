package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.domain.CustomerId;
import com.paymentgateway.payments.domain.OrderId;
import com.paymentgateway.payments.domain.Payment;
import com.paymentgateway.payments.domain.PaymentRef;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.infrastructure.persistence.entity.PaymentReceiptEntity;
import com.paymentgateway.payments.infrastructure.persistence.mapper.PaymentReceiptMapper;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentReceiptRepositoryAdapter implements PaymentReceiptRepository {

    private final PaymentReceiptJpaRepository jpa;
    private final Clock clock;

    public PaymentReceiptRepositoryAdapter(PaymentReceiptJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Payment save(Payment payment) {
        UUID id = payment.getPaymentRef().value();
        PaymentReceiptEntity entity = jpa.findById(id).orElseGet(PaymentReceiptEntity::new);
        PaymentReceiptMapper.mergeAggregate(payment, entity, clock);
        return PaymentReceiptMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Payment> findByPaymentRef(PaymentRef paymentRef) {
        return jpa.findById(paymentRef.value()).map(PaymentReceiptMapper::toDomain);
    }

    @Override
    public List<Payment> findByOrderIdOrderByCreatedAtDesc(OrderId orderId) {
        return jpa.findByOrderIdOrderByCreatedAtDesc(orderId.value()).stream()
                .map(PaymentReceiptMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByCustomerIdOrderByCreatedAtDesc(CustomerId customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId.value()).stream()
                .map(PaymentReceiptMapper::toDomain)
                .toList();
    }
}
