package com.paymentgateway.payments.domain.repository;

import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(UUID eventId);

    List<OutboxEvent> leaseReadyEvents(Instant now, int limit);
}
