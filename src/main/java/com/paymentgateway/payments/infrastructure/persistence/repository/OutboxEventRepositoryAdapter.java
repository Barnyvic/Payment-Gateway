package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.repository.OutboxEventRepository;
import com.paymentgateway.payments.infrastructure.persistence.entity.OutboxEventEntity;
import com.paymentgateway.payments.infrastructure.persistence.mapper.OutboxEventMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpa;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = jpa.findById(outboxEvent.getEventId()).orElseGet(OutboxEventEntity::new);
        OutboxEventMapper.merge(outboxEvent, entity);
        OutboxEventEntity saved = jpa.saveAndFlush(entity);
        OutboxEventEntity reloaded = jpa.findById(saved.getEventId()).orElse(saved);
        return OutboxEventMapper.toDomain(reloaded);
    }

    @Override
    public Optional<OutboxEvent> findById(UUID eventId) {
        return jpa.findById(eventId).map(OutboxEventMapper::toDomain);
    }

    @Override
    @Transactional
    public List<OutboxEvent> leaseReadyEvents(Instant now, int limit) {
        return jpa.leaseReadyEvents(now, limit).stream()
                .map(OutboxEventMapper::toDomain)
                .toList();
    }
}
