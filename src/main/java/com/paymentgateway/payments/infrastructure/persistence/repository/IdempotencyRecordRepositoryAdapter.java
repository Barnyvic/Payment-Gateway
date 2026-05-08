package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;
import com.paymentgateway.payments.domain.repository.IdempotencyRecordRepository;
import com.paymentgateway.payments.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.paymentgateway.payments.infrastructure.persistence.mapper.IdempotencyRecordMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRecordRepositoryAdapter implements IdempotencyRecordRepository {

    private final IdempotencyRecordJpaRepository jpa;

    public IdempotencyRecordRepositoryAdapter(IdempotencyRecordJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        IdempotencyRecordEntity entity = jpa.findById(record.getId()).orElseGet(IdempotencyRecordEntity::new);
        IdempotencyRecordMapper.merge(record, entity);
        IdempotencyRecordEntity saved = jpa.saveAndFlush(entity);
        IdempotencyRecordEntity reloaded = jpa.findById(saved.getId()).orElse(saved);
        return IdempotencyRecordMapper.toDomain(reloaded);
    }

    @Override
    public Optional<IdempotencyRecord> findByOperationAndKey(PaymentOperation operation, String idempotencyKey) {
        return jpa.findByOperationAndIdempotencyKey(operation, idempotencyKey).map(IdempotencyRecordMapper::toDomain);
    }

    @Override
    public Optional<IdempotencyRecord> findById(UUID id) {
        return jpa.findById(id).map(IdempotencyRecordMapper::toDomain);
    }
}
