package com.paymentgateway.payments.infrastructure.jobs.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.outbox.model.OutboxEventType;
import com.paymentgateway.payments.domain.repository.OutboxEventRepository;
import com.paymentgateway.payments.infrastructure.bank.BankClient;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OutboxWorkerExecutor {
    private static final int DEFAULT_BATCH_SIZE = 20;
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(30);

    private final OutboxEventRepository outboxEventRepository;
    private final BankClient bankClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final OutboxBankCompletionPort outboxBankCompletionPort;

    public OutboxWorkerExecutor(
            OutboxEventRepository outboxEventRepository,
            BankClient bankClient,
            ObjectMapper objectMapper,
            Clock clock,
            OutboxBankCompletionPort outboxBankCompletionPort) {
        this.outboxEventRepository = outboxEventRepository;
        this.bankClient = bankClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.outboxBankCompletionPort = outboxBankCompletionPort;
    }

    public int processBatch() {
        return processBatch(DEFAULT_BATCH_SIZE);
    }

    public int processBatch(int limit) {
        Instant now = Instant.now(clock);
        List<OutboxEvent> leased = outboxEventRepository.leaseReadyEvents(now, limit);
        leased.forEach(this::processLeasedEvent);
        return leased.size();
    }

    private void processLeasedEvent(OutboxEvent event) {
        Instant now = Instant.now(clock);
        try {
            executeBankAndComplete(event);
            outboxEventRepository.markProcessed(event.getEventId(), now);
        } catch (BankClientException ex) {
            if (ex.getDetails().category() == BankErrorCategory.TRANSIENT) {
                Instant nextAttemptAt = now.plus(DEFAULT_RETRY_DELAY);
                outboxEventRepository.markRetryableFailure(
                        event.getEventId(),
                        ex.getDetails().code(),
                        ex.getDetails().message(),
                        nextAttemptAt,
                        now);
                return;
            }
            outboxEventRepository.markTerminalFailure(
                    event.getEventId(),
                    ex.getDetails().code(),
                    ex.getDetails().message(),
                    now);
        } catch (OutboxPayloadException ex) {
            outboxEventRepository.markTerminalFailure(
                    event.getEventId(), "invalid_outbox_payload", ex.getMessage(), now);
        } catch (IllegalArgumentException ex) {
            outboxEventRepository.markTerminalFailure(
                    event.getEventId(), "unsupported_outbox_event_type", ex.getMessage(), now);
        } catch (Exception ex) {
            Instant nextAttemptAt = now.plus(DEFAULT_RETRY_DELAY);
            outboxEventRepository.markRetryableFailure(
                    event.getEventId(),
                    "worker_error",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    nextAttemptAt,
                    now);
        }
    }

    private void executeBankAndComplete(OutboxEvent event) {
        String bankIdempotencyKey = buildBankIdempotencyKey(event);
        switch (event.getEventType()) {
            case PAYMENT_AUTHORIZE_REQUESTED -> throw new IllegalArgumentException(
                    "PAYMENT_AUTHORIZE_REQUESTED outbox events are no longer supported; authorization is synchronous.");
            case PAYMENT_CAPTURE_REQUESTED -> {
                CaptureOutboxPayload payload = readPayload(event.getPayload(), CaptureOutboxPayload.class);
                var response = bankClient.capture(
                        new BankCaptureRequest(payload.authorizationId(), payload.amountCents(), payload.currency()),
                        bankIdempotencyKey);
                outboxBankCompletionPort.recordCapture(event.getPaymentRef(), response);
            }
            case PAYMENT_VOID_REQUESTED -> {
                VoidOutboxPayload payload = readPayload(event.getPayload(), VoidOutboxPayload.class);
                var response = bankClient.voidAuthorization(new BankVoidRequest(payload.authorizationId()), bankIdempotencyKey);
                outboxBankCompletionPort.recordVoid(event.getPaymentRef(), response);
            }
            case PAYMENT_REFUND_REQUESTED -> {
                RefundOutboxPayload payload = readPayload(event.getPayload(), RefundOutboxPayload.class);
                var response = bankClient.refund(
                        new BankRefundRequest(payload.captureId(), payload.amountCents(), payload.currency()),
                        bankIdempotencyKey);
                outboxBankCompletionPort.recordRefund(event.getPaymentRef(), response);
            }
        }
    }

    private <T> T readPayload(String payload, Class<T> payloadType) {
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (Exception ex) {
            throw new OutboxPayloadException("Invalid payload for " + payloadType.getSimpleName(), ex);
        }
    }

    private String buildBankIdempotencyKey(OutboxEvent event) {
        return "bank:" + event.getPaymentRef().value() + ":" + event.getEventType().name().toLowerCase() + ":" + event.getEventId();
    }
}
