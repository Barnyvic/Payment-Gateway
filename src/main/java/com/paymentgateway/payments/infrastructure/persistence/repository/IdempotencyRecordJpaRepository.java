package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.infrastructure.persistence.entity.IdempotencyRecordEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

    Optional<IdempotencyRecordEntity> findByOperationAndIdempotencyKey(
            PaymentAction operation, String idempotencyKey);
}
