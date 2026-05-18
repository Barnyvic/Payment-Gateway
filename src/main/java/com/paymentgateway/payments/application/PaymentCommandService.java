package com.paymentgateway.payments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.paymentgateway.payments.domain.exception.IdempotencyInProgressException;
import com.paymentgateway.payments.domain.exception.InvalidPaymentTransitionException;
import com.paymentgateway.payments.domain.exception.MissingIdempotencyKeyException;
import com.paymentgateway.payments.domain.exception.PaymentAwaitingBankException;
import com.paymentgateway.payments.domain.exception.PaymentNotFoundException;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyStatus;
import com.paymentgateway.payments.domain.idempotency.model.PaymentOperation;
import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.model.PaymentCommand;
import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.outbox.model.OutboxEventType;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.IdempotencyRecordRepository;
import com.paymentgateway.payments.domain.repository.OutboxEventRepository;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import com.paymentgateway.payments.infrastructure.jobs.outbox.AuthorizeOutboxPayload;
import com.paymentgateway.payments.infrastructure.jobs.outbox.CaptureOutboxPayload;
import com.paymentgateway.payments.infrastructure.jobs.outbox.RefundOutboxPayload;
import com.paymentgateway.payments.infrastructure.jobs.outbox.VoidOutboxPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandService {

    private static final SupportedCurrency GATEWAY_CURRENCY = SupportedCurrency.USD;

    private final PaymentReceiptRepository paymentReceiptRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRequestHasher paymentRequestHasher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentCommandService(
            PaymentReceiptRepository paymentReceiptRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentRequestHasher paymentRequestHasher,
            ObjectMapper objectMapper,
            Clock clock) {
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentRequestHasher = paymentRequestHasher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public PaymentCommandDispatchResult authorize(String idempotencyKey, AuthorizePaymentCommand body) {
        validateAuthorizeBody(body);
        String key = requireIdempotencyKey(idempotencyKey);
        String hash = paymentRequestHasher.sha256Hex(toAuthorizeHashPayload(body));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentOperation.AUTHORIZE, key, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency = IdempotencyRecord.start(idempotencyId, PaymentOperation.AUTHORIZE, key, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentOperation.AUTHORIZE, key, hash);
        }

        PaymentRef paymentRef = PaymentRef.generate();
        Payment payment = Payment.pending(
                paymentRef,
                new OrderId(body.orderId()),
                new CustomerId(body.customerId()),
                new Money(body.amountCents(), GATEWAY_CURRENCY));
        paymentReceiptRepository.save(payment);

        String outboxPayload = writeJson(
                new AuthorizeOutboxPayload(
                        body.card().number(),
                        body.card().cvv(),
                        body.card().expiryMonth(),
                        body.card().expiryYear(),
                        body.amountCents(),
                        GATEWAY_CURRENCY.name()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, OutboxEventType.PAYMENT_AUTHORIZE_REQUESTED, outboxPayload, now));

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.PENDING.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    @Transactional
    public PaymentCommandDispatchResult capture(String idempotencyKey, PaymentRef paymentRef) {
        String key = requireIdempotencyKey(idempotencyKey);
        String hash = paymentRequestHasher.sha256Hex(new MutationHashPayload(paymentRef.value().toString()));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentOperation.CAPTURE, key, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        PaymentReceiptRecord receipt = loadReceiptOrThrow(paymentRef);
        if (receipt.state() != PaymentState.AUTHORIZED) {
            throw new InvalidPaymentTransitionException(receipt.state(), PaymentCommand.CAPTURE);
        }
        if (receipt.bankAuthorizationId() == null || receipt.bankAuthorizationId().isBlank()) {
            throw new PaymentAwaitingBankException("Capture is not allowed until authorization completes at the bank.");
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency = IdempotencyRecord.start(idempotencyId, PaymentOperation.CAPTURE, key, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentOperation.CAPTURE, key, hash);
        }

        String outboxPayload = writeJson(new CaptureOutboxPayload(
                receipt.bankAuthorizationId(), receipt.amountCents(), receipt.currency().name()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, OutboxEventType.PAYMENT_CAPTURE_REQUESTED, outboxPayload, now));

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.AUTHORIZED.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    @Transactional
    public PaymentCommandDispatchResult voidPayment(String idempotencyKey, PaymentRef paymentRef) {
        String key = requireIdempotencyKey(idempotencyKey);
        String hash = paymentRequestHasher.sha256Hex(new MutationHashPayload(paymentRef.value().toString()));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentOperation.VOID, key, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        PaymentReceiptRecord receipt = loadReceiptOrThrow(paymentRef);
        if (receipt.state() != PaymentState.AUTHORIZED) {
            throw new InvalidPaymentTransitionException(receipt.state(), PaymentCommand.VOID);
        }
        if (receipt.bankAuthorizationId() == null || receipt.bankAuthorizationId().isBlank()) {
            throw new PaymentAwaitingBankException("Void is not allowed until authorization completes at the bank.");
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency = IdempotencyRecord.start(idempotencyId, PaymentOperation.VOID, key, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentOperation.VOID, key, hash);
        }

        String outboxPayload = writeJson(new VoidOutboxPayload(receipt.bankAuthorizationId()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, OutboxEventType.PAYMENT_VOID_REQUESTED, outboxPayload, now));

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.AUTHORIZED.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    @Transactional
    public PaymentCommandDispatchResult refund(String idempotencyKey, PaymentRef paymentRef) {
        String key = requireIdempotencyKey(idempotencyKey);
        String hash = paymentRequestHasher.sha256Hex(new MutationHashPayload(paymentRef.value().toString()));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentOperation.REFUND, key, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        PaymentReceiptRecord receipt = loadReceiptOrThrow(paymentRef);
        if (receipt.state() != PaymentState.CAPTURED) {
            throw new InvalidPaymentTransitionException(receipt.state(), PaymentCommand.REFUND);
        }
        if (receipt.bankCaptureId() == null || receipt.bankCaptureId().isBlank()) {
            throw new PaymentAwaitingBankException("Refund is not allowed until capture completes at the bank.");
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency = IdempotencyRecord.start(idempotencyId, PaymentOperation.REFUND, key, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentOperation.REFUND, key, hash);
        }

        String outboxPayload = writeJson(new RefundOutboxPayload(
                receipt.bankCaptureId(), receipt.amountCents(), receipt.currency().name()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, OutboxEventType.PAYMENT_REFUND_REQUESTED, outboxPayload, now));

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.CAPTURED.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    private void validateAuthorizeBody(AuthorizePaymentCommand body) {
        if (body.orderId() == null || body.orderId().isBlank()) {
            throw new IllegalArgumentException("orderId must be non-blank");
        }
        if (body.customerId() == null || body.customerId().isBlank()) {
            throw new IllegalArgumentException("customerId must be non-blank");
        }
        if (body.amountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }
        CardPayload c = body.card();
        if (c == null) {
            throw new IllegalArgumentException("card is required");
        }
        if (c.number() == null || c.number().isBlank()) {
            throw new IllegalArgumentException("card.number must be non-blank");
        }
        if (c.cvv() == null || c.cvv().isBlank()) {
            throw new IllegalArgumentException("card.cvv must be non-blank");
        }
        if (c.expiryMonth() == null || c.expiryMonth().isBlank()) {
            throw new IllegalArgumentException("card.expiryMonth must be non-blank");
        }
        if (c.expiryYear() == null || c.expiryYear().isBlank()) {
            throw new IllegalArgumentException("card.expiryYear must be non-blank");
        }
    }

    private PaymentReceiptRecord loadReceiptOrThrow(PaymentRef paymentRef) {
        return paymentReceiptRepository
                .findReceiptRecordByPaymentRef(paymentRef)
                .orElseThrow(() -> new PaymentNotFoundException(paymentRef));
    }

    private Optional<PaymentCommandDispatchResult> tryReplay(PaymentOperation operation, String idempotencyKey, String requestHash) {
        return idempotencyRecordRepository
                .findByOperationAndKey(operation, idempotencyKey)
                .map(record -> replayAfterVerify(record, requestHash));
    }

    private PaymentCommandDispatchResult replayAfterVerify(IdempotencyRecord record, String requestHash) {
        record.ensureRequestHashMatches(requestHash);
        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new IdempotencyInProgressException(record.getOperation(), record.getIdempotencyKey());
        }
        String snapshot = record.getResponseSnapshot().orElse("{}");
        return PaymentCommandDispatchResult.replayed200(snapshot);
    }

    private PaymentCommandDispatchResult replayAfterConcurrentInsert(PaymentOperation operation, String idempotencyKey, String requestHash) {
        return idempotencyRecordRepository
                .findByOperationAndKey(operation, idempotencyKey)
                .map(record -> replayAfterVerify(record, requestHash))
                .orElseThrow(() -> new IllegalStateException("Lost race on idempotency insert without readable record"));
    }

    private static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        return idempotencyKey.strip();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private static AuthorizeHashPayload toAuthorizeHashPayload(AuthorizePaymentCommand body) {
        return new AuthorizeHashPayload(
                body.orderId(),
                body.customerId(),
                body.card().number(),
                body.card().cvv(),
                body.card().expiryMonth(),
                body.card().expiryYear(),
                body.amountCents());
    }

    private record AuthorizeHashPayload(
            String orderId,
            String customerId,
            String cardNumber,
            String cvv,
            String expiryMonth,
            String expiryYear,
            long amountCents) {}

    private record MutationHashPayload(String paymentRef) {}

    public record AuthorizePaymentCommand(String orderId, String customerId, CardPayload card, long amountCents) {}

    public record CardPayload(String number, String cvv, String expiryMonth, String expiryYear) {}

    public record AsyncCommandAcceptedBody(UUID paymentRef, String receiptStateAtEnqueue) {}
}
