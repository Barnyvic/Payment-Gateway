package com.paymentgateway.payments.domain.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentgateway.payments.domain.idempotency.exception.IdempotencyConflictException;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyStatus;
import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;
import com.paymentgateway.payments.domain.value.PaymentRef;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyRecordTest {

    @Test
    void accepted_storesSnapshotAndPaymentRef() {
        Instant now = Instant.parse("2026-05-08T20:00:00Z");
        PaymentRef ref = PaymentRef.generate();
        IdempotencyRecord record =
                IdempotencyRecord.accepted(
                        UUID.randomUUID(),
                        PaymentOperation.AUTHORIZE,
                        "key-a",
                        "hash-a",
                        ref,
                        "{\"paymentRef\":\"" + ref.value() + "\"}",
                        now);

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.ACCEPTED);
        assertThat(record.getPaymentRef()).contains(ref);
        assertThat(record.getResponseSnapshot()).isPresent();
    }

    @Test
    void acceptAsyncCommand_transitionsToAccepted() {
        Instant now = Instant.parse("2026-05-08T20:00:00Z");
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.AUTHORIZE, "key-ac", "hash-ac", now);
        PaymentRef ref = PaymentRef.generate();
        Instant done = Instant.parse("2026-05-08T20:00:01Z");

        record.acceptAsyncCommand(ref, "{\"ok\":true}", done);

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.ACCEPTED);
        assertThat(record.getPaymentRef()).contains(ref);
        assertThat(record.getResponseSnapshot()).contains("{\"ok\":true}");
        assertThat(record.getUpdatedAt()).isEqualTo(done);
    }

    @Test
    void start_setsInProgressAndTimestamps() {
        Instant now = Instant.parse("2026-05-08T20:00:00Z");
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.AUTHORIZE, "key-1", "hash-1", now);

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
        assertThat(record.getCreatedAt()).isEqualTo(now);
        assertThat(record.getUpdatedAt()).isEqualTo(now);
        assertThat(record.getResponseSnapshot()).isEmpty();
        assertThat(record.getPaymentRef()).isEmpty();
    }

    @Test
    void ensureRequestHashMatches_allowsSameHash() {
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.CAPTURE, "key-2", "hash-2", Instant.now());

        record.ensureRequestHashMatches("hash-2");
    }

    @Test
    void ensureRequestHashMatches_throwsOnMismatch() {
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.REFUND, "key-3", "hash-3", Instant.now());

        assertThatThrownBy(() -> record.ensureRequestHashMatches("another-hash"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("key-3");
    }

    @Test
    void markSucceeded_storesResponseAndPaymentRef() {
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.AUTHORIZE, "key-4", "hash-4", Instant.now());
        PaymentRef paymentRef = PaymentRef.generate();
        Instant doneAt = Instant.parse("2026-05-08T20:05:00Z");

        record.markSucceeded("{\"status\":\"ok\"}", paymentRef, doneAt);

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.SUCCEEDED);
        assertThat(record.getResponseSnapshot()).contains("{\"status\":\"ok\"}");
        assertThat(record.getPaymentRef()).contains(paymentRef);
        assertThat(record.getUpdatedAt()).isEqualTo(doneAt);
    }

    @Test
    void markFailed_storesFailureSnapshot() {
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.VOID, "key-5", "hash-5", Instant.now());
        Instant failedAt = Instant.parse("2026-05-08T20:10:00Z");

        record.markFailed("{\"error\":\"timeout\"}", failedAt);

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
        assertThat(record.getResponseSnapshot()).contains("{\"error\":\"timeout\"}");
        assertThat(record.getUpdatedAt()).isEqualTo(failedAt);
    }

    @Test
    void markSucceeded_failsWhenAlreadyTerminal() {
        IdempotencyRecord record =
                IdempotencyRecord.start(UUID.randomUUID(), PaymentOperation.VOID, "key-6", "hash-6", Instant.now());
        record.markFailed("{\"error\":\"declined\"}", Instant.now());

        assertThatThrownBy(() -> record.markSucceeded("{\"ok\":true}", PaymentRef.generate(), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void rehydrate_failsWhenUpdatedAtBeforeCreatedAt() {
        Instant created = Instant.parse("2026-05-08T20:10:00Z");
        Instant updated = Instant.parse("2026-05-08T20:09:59Z");

        assertThatThrownBy(() ->
                        IdempotencyRecord.rehydrate(
                                UUID.randomUUID(),
                                PaymentOperation.AUTHORIZE,
                                "key-7",
                                "hash-7",
                                IdempotencyStatus.IN_PROGRESS,
                                null,
                                null,
                                created,
                                updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("updatedAt");
    }

    @Test
    void rehydrate_failsWhenSucceededWithoutPaymentRef() {
        Instant now = Instant.parse("2026-05-08T20:15:00Z");

        assertThatThrownBy(() ->
                        IdempotencyRecord.rehydrate(
                                UUID.randomUUID(),
                                PaymentOperation.AUTHORIZE,
                                "key-8",
                                "hash-8",
                                IdempotencyStatus.SUCCEEDED,
                                "{\"status\":\"ok\"}",
                                null,
                                now,
                                now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentRef");
    }
}
