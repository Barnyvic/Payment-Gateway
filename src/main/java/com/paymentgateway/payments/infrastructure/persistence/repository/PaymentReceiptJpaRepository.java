package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.infrastructure.persistence.entity.PaymentReceiptEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReceiptJpaRepository extends JpaRepository<PaymentReceiptEntity, UUID> {

    List<PaymentReceiptEntity> findByOrderIdOrderByCreatedAtDesc(String orderId);

    List<PaymentReceiptEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<PaymentReceiptEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
}
