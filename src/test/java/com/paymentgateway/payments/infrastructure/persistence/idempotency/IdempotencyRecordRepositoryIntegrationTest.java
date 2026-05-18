package com.paymentgateway.payments.infrastructure.persistence.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentgateway.AbstractPostgresIntegrationTest;
import com.paymentgateway.gateway.PaymentGatewayApplication;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyStatus;
import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.domain.repository.IdempotencyRecordRepository;
import com.paymentgateway.payments.domain.value.PaymentRef;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = PaymentGatewayApplication.class)
@Transactional
class IdempotencyRecordRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdempotencyRecordRepository repository;

    @Test
    void saveAndFindByOperationAndKey_roundTripsInProgressRecord() {
        Instant now = Instant.parse("2026-05-08T21:00:00Z");
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentAction.AUTHORIZE, "idem-key-1", "hash-1", now);

        repository.save(record);

        assertThat(repository.findByOperationAndKey(PaymentAction.AUTHORIZE, "idem-key-1"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getRequestHash()).isEqualTo("hash-1");
                    assertThat(found.getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
                    assertThat(found.getResponseSnapshot()).isEmpty();
                });
    }

    @Test
    void saveAfterSuccess_persistsResponseAndPaymentRef() {
        IdempotencyRecord record = IdempotencyRecord.start(
                UUID.randomUUID(),
                PaymentAction.CAPTURE,
                "idem-key-2",
                "hash-2",
                Instant.parse("2026-05-08T21:10:00Z"));
        IdempotencyRecord saved = repository.save(record);

        PaymentRef paymentRef = PaymentRef.generate();
        saved.markSucceeded("{\"status\":\"captured\"}", paymentRef, Instant.parse("2026-05-08T21:11:00Z"));
        repository.save(saved);

        assertThat(repository.findById(saved.getId())).hasValueSatisfying(found -> {
            assertThat(found.getStatus()).isEqualTo(IdempotencyStatus.SUCCEEDED);
            assertThat(found.getResponseSnapshot()).contains("{\"status\":\"captured\"}");
            assertThat(found.getPaymentRef()).contains(paymentRef);
        });
    }
}
