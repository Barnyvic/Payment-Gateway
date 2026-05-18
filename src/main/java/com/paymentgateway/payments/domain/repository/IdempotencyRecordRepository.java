package com.paymentgateway.payments.domain.repository;

import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.common.util.PaymentAction;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository {

    IdempotencyRecord save(IdempotencyRecord record);

    Optional<IdempotencyRecord> findByOperationAndKey(PaymentAction operation, String idempotencyKey);

    Optional<IdempotencyRecord> findById(UUID id);
}
