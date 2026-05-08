package com.paymentgateway.payments.domain.repository;

import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository {

    IdempotencyRecord save(IdempotencyRecord record);

    Optional<IdempotencyRecord> findByOperationAndKey(PaymentOperation operation, String idempotencyKey);

    Optional<IdempotencyRecord> findById(UUID id);
}
