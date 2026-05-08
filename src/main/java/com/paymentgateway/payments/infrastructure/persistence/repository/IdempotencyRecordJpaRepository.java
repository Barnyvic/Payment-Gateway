package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;
import com.paymentgateway.payments.infrastructure.persistence.entity.IdempotencyRecordEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

    Optional<IdempotencyRecordEntity> findByOperationAndIdempotencyKey(
            PaymentOperation operation, String idempotencyKey);
}
