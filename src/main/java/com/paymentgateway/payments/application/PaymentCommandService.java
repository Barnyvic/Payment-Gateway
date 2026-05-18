package com.paymentgateway.payments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.paymentgateway.payments.application.exception.PaymentAuthorizationException;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort;
import com.paymentgateway.common.util.PaymentRequestHasher;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort.AuthorizeBankCommand;
import com.paymentgateway.payments.domain.exception.IdempotencyInProgressException;
import com.paymentgateway.payments.domain.exception.InvalidPaymentTransitionException;
import com.paymentgateway.payments.domain.exception.PaymentAwaitingBankException;
import com.paymentgateway.payments.domain.exception.PaymentNotFoundException;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyRecord;
import com.paymentgateway.payments.domain.idempotency.model.IdempotencyStatus;
import com.paymentgateway.common.util.PaymentAction;
import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.IdempotencyRecordRepository;
import com.paymentgateway.payments.domain.repository.OutboxEventRepository;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import com.paymentgateway.payments.infrastructure.jobs.outbox.CaptureOutboxPayload;
import com.paymentgateway.payments.infrastructure.jobs.outbox.RefundOutboxPayload;
import com.paymentgateway.payments.infrastructure.jobs.outbox.VoidOutboxPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandService.class);

    private static final SupportedCurrency GATEWAY_CURRENCY = SupportedCurrency.USD;

    private final PaymentReceiptRepository paymentReceiptRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRequestHasher paymentRequestHasher;
    private final PaymentAuthorizationPort paymentAuthorizationPort;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentCommandService(
            PaymentReceiptRepository paymentReceiptRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentRequestHasher paymentRequestHasher,
            PaymentAuthorizationPort paymentAuthorizationPort,
            ObjectMapper objectMapper,
            Clock clock) {
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentRequestHasher = paymentRequestHasher;
        this.paymentAuthorizationPort = paymentAuthorizationPort;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public PaymentCommandDispatchResult authorize(String idempotencyKey, AuthorizePaymentCommand body) {
        log.info(
                "Authorize requested orderId={} customerId={} amountCents={}",
                body.orderId(),
                body.customerId(),
                body.amountCents());
        String hash = paymentRequestHasher.sha256Hex(toAuthorizeHashPayload(body));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentAction.AUTHORIZE, idempotencyKey, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency =
                IdempotencyRecord.start(idempotencyId, PaymentAction.AUTHORIZE, idempotencyKey, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentAction.AUTHORIZE, idempotencyKey, hash);
        }

        PaymentRef paymentRef = PaymentRef.generate();
        Payment payment = Payment.pending(
                paymentRef,
                new OrderId(body.orderId()),
                new CustomerId(body.customerId()),
                new Money(body.amountCents(), GATEWAY_CURRENCY));
        paymentReceiptRepository.save(payment);

        String bankIdempotencyKey = buildAuthorizeBankIdempotencyKey(paymentRef, idempotencyKey);
        AuthorizeBankCommand bankCommand = new AuthorizeBankCommand(
                body.card().number(),
                body.card().cvv(),
                body.card().expiryMonth(),
                body.card().expiryYear(),
                body.amountCents(),
                GATEWAY_CURRENCY);
        try {
            var authorizeResult = paymentAuthorizationPort.authorize(bankCommand, bankIdempotencyKey);
            paymentReceiptRepository.recordAuthorizeSuccess(paymentRef, authorizeResult.authorizationId());
            log.info(
                    "Authorize succeeded paymentRef={} bankAuthorizationId={}",
                    paymentRef.value(),
                    authorizeResult.authorizationId());
        } catch (PaymentAuthorizationException ex) {
            if (ex.isTransientFailure()) {
                log.warn(
                        "Authorize failed transiently paymentRef={} code={}",
                        paymentRef.value(),
                        ex.getErrorCode());
                throw ex;
            }
            log.warn(
                    "Authorize declined paymentRef={} code={} message={}",
                    paymentRef.value(),
                    ex.getErrorCode(),
                    ex.getMessage());
        }

        PaymentState receiptState = paymentReceiptRepository
                .findReceiptRecordByPaymentRef(paymentRef)
                .map(PaymentReceiptRecord::state)
                .orElse(PaymentState.PENDING);
        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), receiptState.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        log.info("Authorize accepted paymentRef={} receiptState={}", paymentRef.value(), receiptState);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    @Transactional
    public PaymentCommandDispatchResult capture(String idempotencyKey, PaymentRef paymentRef) {
        log.info("Capture requested paymentRef={}", paymentRef.value());
        String hash = paymentRequestHasher.sha256Hex(new MutationHashPayload(paymentRef.value().toString()));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentAction.CAPTURE, idempotencyKey, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        PaymentReceiptRecord receipt = loadReceiptOrThrow(paymentRef);
        if (receipt.state() != PaymentState.AUTHORIZED) {
            throw new InvalidPaymentTransitionException(receipt.state(), PaymentAction.CAPTURE);
        }
        if (receipt.bankAuthorizationId() == null || receipt.bankAuthorizationId().isBlank()) {
            throw new PaymentAwaitingBankException("Capture is not allowed until authorization completes at the bank.");
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency =
                IdempotencyRecord.start(idempotencyId, PaymentAction.CAPTURE, idempotencyKey, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentAction.CAPTURE, idempotencyKey, hash);
        }

        String outboxPayload = writeJson(new CaptureOutboxPayload(
                receipt.bankAuthorizationId(), receipt.amountCents(), receipt.currency().name()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, PaymentAction.CAPTURE, outboxPayload, now));
        log.info("Capture outbox enqueued paymentRef={} eventId={}", paymentRef.value(), eventId);

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.AUTHORIZED.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    @Transactional
    public PaymentCommandDispatchResult voidPayment(String idempotencyKey, PaymentRef paymentRef) {
        log.info("Void requested paymentRef={}", paymentRef.value());
        String hash = paymentRequestHasher.sha256Hex(new MutationHashPayload(paymentRef.value().toString()));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentAction.VOID, idempotencyKey, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        PaymentReceiptRecord receipt = loadReceiptOrThrow(paymentRef);
        if (receipt.state() != PaymentState.AUTHORIZED) {
            throw new InvalidPaymentTransitionException(receipt.state(), PaymentAction.VOID);
        }
        if (receipt.bankAuthorizationId() == null || receipt.bankAuthorizationId().isBlank()) {
            throw new PaymentAwaitingBankException("Void is not allowed until authorization completes at the bank.");
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency =
                IdempotencyRecord.start(idempotencyId, PaymentAction.VOID, idempotencyKey, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentAction.VOID, idempotencyKey, hash);
        }

        String outboxPayload = writeJson(new VoidOutboxPayload(receipt.bankAuthorizationId()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, PaymentAction.VOID, outboxPayload, now));
        log.info("Void outbox enqueued paymentRef={} eventId={}", paymentRef.value(), eventId);

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.AUTHORIZED.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    @Transactional
    public PaymentCommandDispatchResult refund(String idempotencyKey, PaymentRef paymentRef) {
        log.info("Refund requested paymentRef={}", paymentRef.value());
        String hash = paymentRequestHasher.sha256Hex(new MutationHashPayload(paymentRef.value().toString()));
        Optional<PaymentCommandDispatchResult> replay = tryReplay(PaymentAction.REFUND, idempotencyKey, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        PaymentReceiptRecord receipt = loadReceiptOrThrow(paymentRef);
        if (receipt.state() != PaymentState.CAPTURED) {
            throw new InvalidPaymentTransitionException(receipt.state(), PaymentAction.REFUND);
        }
        if (receipt.bankCaptureId() == null || receipt.bankCaptureId().isBlank()) {
            throw new PaymentAwaitingBankException("Refund is not allowed until capture completes at the bank.");
        }

        Instant now = clock.instant();
        UUID idempotencyId = UuidCreator.getTimeOrderedEpoch();
        IdempotencyRecord idempotency =
                IdempotencyRecord.start(idempotencyId, PaymentAction.REFUND, idempotencyKey, hash, now);
        try {
            idempotencyRecordRepository.save(idempotency);
        } catch (DataIntegrityViolationException ex) {
            return replayAfterConcurrentInsert(PaymentAction.REFUND, idempotencyKey, hash);
        }

        String outboxPayload = writeJson(new RefundOutboxPayload(
                receipt.bankCaptureId(), receipt.amountCents(), receipt.currency().name()));
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        outboxEventRepository.save(OutboxEvent.enqueue(eventId, paymentRef, PaymentAction.REFUND, outboxPayload, now));
        log.info("Refund outbox enqueued paymentRef={} eventId={}", paymentRef.value(), eventId);

        String acceptanceJson = writeJson(new AsyncCommandAcceptedBody(paymentRef.value(), PaymentState.CAPTURED.name()));
        idempotency.acceptAsyncCommand(paymentRef, acceptanceJson, now);
        idempotencyRecordRepository.save(idempotency);
        return PaymentCommandDispatchResult.accepted202(acceptanceJson);
    }

    private PaymentReceiptRecord loadReceiptOrThrow(PaymentRef paymentRef) {
        return paymentReceiptRepository
                .findReceiptRecordByPaymentRef(paymentRef)
                .orElseThrow(() -> new PaymentNotFoundException(paymentRef));
    }

    private Optional<PaymentCommandDispatchResult> tryReplay(PaymentAction operation, String idempotencyKey, String requestHash) {
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
        log.debug(
                "Idempotent replay operation={} idempotencyKey={}",
                record.getOperation(),
                record.getIdempotencyKey());
        return PaymentCommandDispatchResult.replayed200(snapshot);
    }

    private PaymentCommandDispatchResult replayAfterConcurrentInsert(PaymentAction operation, String idempotencyKey, String requestHash) {
        return idempotencyRecordRepository
                .findByOperationAndKey(operation, idempotencyKey)
                .map(record -> replayAfterVerify(record, requestHash))
                .orElseThrow(() -> new IllegalStateException("Lost race on idempotency insert without readable record"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private static String buildAuthorizeBankIdempotencyKey(PaymentRef paymentRef, String clientIdempotencyKey) {
        return "bank:" + paymentRef.value() + ":authorize:" + clientIdempotencyKey;
    }

    private static AuthorizeHashPayload toAuthorizeHashPayload(AuthorizePaymentCommand body) {
        return new AuthorizeHashPayload(
                body.orderId(),
                body.customerId(),
                cardLastFour(body.card().number()),
                body.card().expiryMonth(),
                body.card().expiryYear(),
                body.amountCents());
    }

    private static String cardLastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "0000";
        }
        return cardNumber.substring(cardNumber.length() - 4);
    }

    private record AuthorizeHashPayload(
            String orderId,
            String customerId,
            String cardLastFour,
            String expiryMonth,
            String expiryYear,
            long amountCents) {}

    private record MutationHashPayload(String paymentRef) {}

    public record AuthorizePaymentCommand(String orderId, String customerId, CardPayload card, long amountCents) {}

    public record CardPayload(String number, String cvv, String expiryMonth, String expiryYear) {}

    public record AsyncCommandAcceptedBody(UUID paymentRef, String receiptStateAtEnqueue) {}

    public record PaymentCommandDispatchResult(Type type, String responseBodyJson) {

        public enum Type {
            ACCEPTED_202,
            REPLAYED_200
        }

        public static PaymentCommandDispatchResult accepted202(String responseBodyJson) {
            return new PaymentCommandDispatchResult(Type.ACCEPTED_202, responseBodyJson);
        }

        public static PaymentCommandDispatchResult replayed200(String responseBodyJson) {
            return new PaymentCommandDispatchResult(Type.REPLAYED_200, responseBodyJson);
        }
    }
}
