package com.paymentgateway.payments.infrastructure.persistence.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import com.paymentgateway.AbstractPostgresIntegrationTest;
import com.paymentgateway.gateway.PaymentGatewayApplication;
import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.outbox.model.OutboxEventType;
import com.paymentgateway.payments.domain.outbox.model.OutboxStatus;
import com.paymentgateway.payments.domain.repository.OutboxEventRepository;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.persistence.entity.OutboxEventEntity;
import com.paymentgateway.payments.infrastructure.persistence.repository.OutboxEventJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = PaymentGatewayApplication.class)
@Transactional
class OutboxEventRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxEventJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void saveAndFind_roundTripPendingOutboxEvent() {
        Instant now = Instant.parse("2026-05-08T22:00:00Z");
        PaymentRef paymentRef = PaymentRef.generate();
        OutboxEvent event = OutboxEvent.enqueue(
                UuidCreator.getTimeOrderedEpoch(),
                paymentRef,
                OutboxEventType.PAYMENT_AUTHORIZE_REQUESTED,
                "{\"amount\":5000}",
                now);

        OutboxEvent saved = repository.save(event);

        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getEventType()).isEqualTo(OutboxEventType.PAYMENT_AUTHORIZE_REQUESTED);
        assertThat(saved.getPayload()).isEqualTo("{\"amount\":5000}");

        assertThat(repository.findById(saved.getEventId())).hasValueSatisfying(found -> {
            assertThat(found.getPaymentRef()).isEqualTo(paymentRef);
            assertThat(found.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void leaseReadyEvents_returnsOnlyDuePendingEvents() {
        Instant now = Instant.parse("2026-05-09T09:00:00Z");

        OutboxEvent due = OutboxEvent.enqueue(
                UuidCreator.getTimeOrderedEpoch(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_CAPTURE_REQUESTED,
                "{\"step\":\"due\"}",
                now.minusSeconds(30));
        OutboxEvent future = OutboxEvent.enqueue(
                UuidCreator.getTimeOrderedEpoch(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_VOID_REQUESTED,
                "{\"step\":\"future\"}",
                now.plusSeconds(300));

        repository.save(due);
        repository.save(future);

        assertThat(repository.leaseReadyEvents(now, 10)).singleElement().satisfies(leased -> {
            assertThat(leased.getPayload()).isEqualTo("{\"step\":\"due\"}");
            assertThat(leased.getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS);
            assertThat(leased.getAttemptCount()).isEqualTo(1);
        });
    }

    @Test
    void markProcessed_transitionsFromInProgress() {
        Instant now = Instant.parse("2026-05-09T09:30:00Z");
        OutboxEvent event = OutboxEvent.enqueue(
                UuidCreator.getTimeOrderedEpoch(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_CAPTURE_REQUESTED,
                "{\"op\":\"capture\"}",
                now.minusSeconds(1));
        repository.save(event);

        OutboxEvent leased = repository.leaseReadyEvents(now, 1).getFirst();
        boolean changed = repository.markProcessed(leased.getEventId(), now.plusSeconds(1));

        assertThat(changed).isTrue();
        entityManager.clear();
        assertThat(repository.findById(leased.getEventId())).hasValueSatisfying(found -> {
            assertThat(found.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
            assertThat(found.getAttemptCount()).isEqualTo(1);
        });
    }

    @Test
    void markRetryableFailure_reschedulesBeforeMaxAttempts() {
        Instant now = Instant.parse("2026-05-09T10:00:00Z");
        OutboxEvent event = OutboxEvent.enqueue(
                UuidCreator.getTimeOrderedEpoch(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_REFUND_REQUESTED,
                "{\"op\":\"refund\"}",
                now.minusSeconds(10));
        repository.save(event);
        OutboxEvent leased = repository.leaseReadyEvents(now, 1).getFirst();

        Instant nextAttempt = now.plusSeconds(120);
        boolean changed = repository.markRetryableFailure(
                leased.getEventId(), "BANK_TIMEOUT", "timeout", nextAttempt, now.plusSeconds(1));

        assertThat(changed).isTrue();
        entityManager.clear();
        OutboxEventEntity row = jpaRepository.findById(leased.getEventId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(row.getLastErrorCode()).isEqualTo("BANK_TIMEOUT");
        assertThat(row.getLastErrorMessage()).isEqualTo("timeout");
        assertThat(row.getNextAttemptAt()).isEqualTo(nextAttempt);
    }

    @Test
    void markRetryableFailure_marksFailedWhenMaxAttemptsReached() {
        Instant now = Instant.parse("2026-05-09T11:00:00Z");
        OutboxEvent event = OutboxEvent.enqueue(
                UuidCreator.getTimeOrderedEpoch(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_VOID_REQUESTED,
                "{\"op\":\"void\"}",
                now.minusSeconds(10));
        repository.save(event);

        OutboxEventEntity persisted = jpaRepository.findById(event.getEventId()).orElseThrow();
        persisted.setMaxAttempts(1);
        jpaRepository.saveAndFlush(persisted);

        OutboxEvent leased = repository.leaseReadyEvents(now, 1).getFirst();
        boolean changed = repository.markRetryableFailure(
                leased.getEventId(), "BANK_500", "upstream error", now.plusSeconds(120), now.plusSeconds(1));

        assertThat(changed).isTrue();
        entityManager.clear();
        OutboxEventEntity row = jpaRepository.findById(leased.getEventId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(row.getAttemptCount()).isEqualTo(1);
    }
}
