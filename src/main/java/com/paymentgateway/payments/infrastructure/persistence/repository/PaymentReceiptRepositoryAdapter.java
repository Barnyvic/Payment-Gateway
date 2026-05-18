package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.persistence.entity.PaymentReceiptEntity;
import com.paymentgateway.payments.infrastructure.persistence.mapper.PaymentReceiptMapper;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    public List<PaymentReceiptRecord> findReceiptRecordsByOrderId(OrderId orderId) {
        return jpa.findByOrderIdOrderByCreatedAtDesc(orderId.value()).stream()
                .map(PaymentReceiptMapper::toRecord)
                .toList();
    }

    @Override
    public List<PaymentReceiptRecord> findReceiptRecordsByCustomerId(CustomerId customerId, int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId.value(), PageRequest.of(0, capped)).stream()
                .map(PaymentReceiptMapper::toRecord)
                .toList();
    }

    @Override
    public Optional<PaymentReceiptRecord> findReceiptRecordByPaymentRef(PaymentRef paymentRef) {
        return jpa.findById(paymentRef.value()).map(PaymentReceiptMapper::toRecord);
    }

    @Override
    @Transactional
    public void recordAuthorizeSuccess(PaymentRef paymentRef, String bankAuthorizationId) {
        PaymentReceiptEntity entity = jpa.findById(paymentRef.value()).orElseThrow();
        Payment payment = PaymentReceiptMapper.toDomain(entity);
        payment.authorize();
        PaymentReceiptMapper.mergeAggregate(payment, entity, clock);
        entity.setBankAuthorizationId(bankAuthorizationId);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void recordCaptureSuccess(PaymentRef paymentRef, String bankCaptureId) {
        PaymentReceiptEntity entity = jpa.findById(paymentRef.value()).orElseThrow();
        Payment payment = PaymentReceiptMapper.toDomain(entity);
        payment.capture();
        PaymentReceiptMapper.mergeAggregate(payment, entity, clock);
        entity.setBankCaptureId(bankCaptureId);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void recordVoidSuccess(PaymentRef paymentRef, String bankVoidId) {
        PaymentReceiptEntity entity = jpa.findById(paymentRef.value()).orElseThrow();
        Payment payment = PaymentReceiptMapper.toDomain(entity);
        payment.voidAuthorization();
        PaymentReceiptMapper.mergeAggregate(payment, entity, clock);
        entity.setBankVoidId(bankVoidId);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void recordRefundSuccess(PaymentRef paymentRef, String bankRefundId) {
        PaymentReceiptEntity entity = jpa.findById(paymentRef.value()).orElseThrow();
        Payment payment = PaymentReceiptMapper.toDomain(entity);
        payment.refund();
        PaymentReceiptMapper.mergeAggregate(payment, entity, clock);
        entity.setBankRefundId(bankRefundId);
        jpa.save(entity);
    }

    @Override
    public List<Payment> findByCustomerIdOrderByCreatedAtDesc(CustomerId customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId.value()).stream()
                .map(PaymentReceiptMapper::toDomain)
                .toList();
    }
}
