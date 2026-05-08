package com.paymentgateway.payments.infrastructure.persistence.mapper;

import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.persistence.entity.IdempotencyRecordEntity;

public final class IdempotencyRecordMapper {

    private IdempotencyRecordMapper() {}

    public static IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
        return IdempotencyRecord.rehydrate(
                entity.getId(),
                entity.getOperation(),
                entity.getIdempotencyKey(),
                entity.getRequestHash(),
                entity.getStatus(),
                entity.getResponseSnapshot(),
                entity.getPaymentRef() == null ? null : new PaymentRef(entity.getPaymentRef()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static void merge(IdempotencyRecord record, IdempotencyRecordEntity entity) {
        entity.setId(record.getId());
        entity.setOperation(record.getOperation());
        entity.setIdempotencyKey(record.getIdempotencyKey());
        entity.setRequestHash(record.getRequestHash());
        entity.setStatus(record.getStatus());
        entity.setResponseSnapshot(record.getResponseSnapshot().orElse(null));
        entity.setPaymentRef(record.getPaymentRef().map(PaymentRef::value).orElse(null));
    }
}
