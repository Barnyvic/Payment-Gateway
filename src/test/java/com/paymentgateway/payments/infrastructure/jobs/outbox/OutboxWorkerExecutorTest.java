package com.paymentgateway.payments.infrastructure.jobs.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payments.domain.outbox.model.OutboxEvent;
import com.paymentgateway.payments.domain.outbox.model.OutboxEventType;
import com.paymentgateway.payments.domain.outbox.model.OutboxStatus;
import com.paymentgateway.payments.domain.repository.OutboxEventRepository;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.bank.BankClient;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerExecutorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private BankClient bankClient;

    @Mock
    private OutboxBankCompletionPort outboxBankCompletionPort;

    private OutboxWorkerExecutor executor;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC);
        executor = new OutboxWorkerExecutor(
                outboxEventRepository, bankClient, new ObjectMapper(), clock, outboxBankCompletionPort);
    }

    @Test
    void processBatch_shouldDispatchAuthorizeAndMarkProcessed() {
        OutboxEvent event = OutboxEvent.rehydrate(
                UUID.randomUUID(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_AUTHORIZE_REQUESTED,
                """
                {"cardNumber":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2030","amountCents":5000,"currency":"USD"}
                """,
                OutboxStatus.IN_PROGRESS,
                1,
                Instant.parse("2026-05-09T11:59:00Z"),
                Instant.parse("2026-05-09T11:58:00Z"),
                Instant.parse("2026-05-09T11:59:00Z"));
        when(outboxEventRepository.leaseReadyEvents(any(), eq(5))).thenReturn(List.of(event));
        when(bankClient.authorize(any(), any())).thenReturn(new BankAuthorizeResponse("bank-auth-1", "AUTHORIZED"));

        int processed = executor.processBatch(5);

        ArgumentCaptor<BankAuthorizeRequest> requestCaptor = ArgumentCaptor.forClass(BankAuthorizeRequest.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(bankClient).authorize(requestCaptor.capture(), keyCaptor.capture());
        verify(outboxBankCompletionPort)
                .recordAuthorize(eq(event.getPaymentRef()), org.mockito.ArgumentMatchers.any(BankAuthorizeResponse.class));
        verify(outboxEventRepository).markProcessed(eq(event.getEventId()), eq(Instant.parse("2026-05-09T12:00:00Z")));
        verify(outboxEventRepository, never()).markRetryableFailure(any(), any(), any(), any(), any());

        BankAuthorizeRequest request = requestCaptor.getValue();
        Assertions.assertEquals("4111111111111111", request.cardNumber());
        Assertions.assertEquals(5000L, request.amountCents());
        Assertions.assertEquals(
                "bank:" + event.getPaymentRef().value() + ":payment_authorize_requested:" + event.getEventId(),
                keyCaptor.getValue());
        Assertions.assertEquals(1, processed);
    }

    @Test
    void processBatch_shouldScheduleRetryForTransientBankError() {
        OutboxEvent event = OutboxEvent.rehydrate(
                UUID.randomUUID(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_CAPTURE_REQUESTED,
                """
                {"authorizationId":"auth_123","amountCents":5000,"currency":"USD"}
                """,
                OutboxStatus.IN_PROGRESS,
                2,
                Instant.parse("2026-05-09T11:59:00Z"),
                Instant.parse("2026-05-09T11:58:00Z"),
                Instant.parse("2026-05-09T11:59:00Z"));
        when(outboxEventRepository.leaseReadyEvents(any(), eq(10))).thenReturn(List.of(event));
        when(bankClient.capture(any(), any()))
                .thenThrow(
                        new BankClientException(new BankErrorDetails(
                                "bank_timeout", "timeout", 503, BankErrorCategory.TRANSIENT, true)));

        executor.processBatch(10);

        verify(outboxEventRepository).markRetryableFailure(
                eq(event.getEventId()),
                eq("bank_timeout"),
                eq("timeout"),
                eq(Instant.parse("2026-05-09T12:00:30Z")),
                eq(Instant.parse("2026-05-09T12:00:00Z")));
        verify(outboxEventRepository, never()).markProcessed(any(), any());
    }

    @Test
    void processBatch_shouldMarkTerminalFailureForPermanentBankError() {
        OutboxEvent event = OutboxEvent.rehydrate(
                UUID.randomUUID(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_VOID_REQUESTED,
                """
                {"authorizationId":"auth_123"}
                """,
                OutboxStatus.IN_PROGRESS,
                1,
                Instant.parse("2026-05-09T11:59:00Z"),
                Instant.parse("2026-05-09T11:58:00Z"),
                Instant.parse("2026-05-09T11:59:00Z"));
        when(outboxEventRepository.leaseReadyEvents(any(), eq(10))).thenReturn(List.of(event));
        when(bankClient.voidAuthorization(any(), any()))
                .thenThrow(new BankClientException(
                        new BankErrorDetails("invalid_state", "Cannot void captured payment", 422, BankErrorCategory.PERMANENT, false)));

        executor.processBatch(10);

        verify(outboxEventRepository).markTerminalFailure(
                eq(event.getEventId()),
                eq("invalid_state"),
                eq("Cannot void captured payment"),
                eq(Instant.parse("2026-05-09T12:00:00Z")));
        verify(outboxEventRepository, never()).markRetryableFailure(any(), any(), any(), any(), any());
    }

    @Test
    void processBatch_shouldMarkTerminalFailureForInvalidPayload() {
        OutboxEvent event = OutboxEvent.rehydrate(
                UUID.randomUUID(),
                PaymentRef.generate(),
                OutboxEventType.PAYMENT_CAPTURE_REQUESTED,
                "{\"authorizationId\":}",
                OutboxStatus.IN_PROGRESS,
                1,
                Instant.parse("2026-05-09T11:59:00Z"),
                Instant.parse("2026-05-09T11:58:00Z"),
                Instant.parse("2026-05-09T11:59:00Z"));
        when(outboxEventRepository.leaseReadyEvents(any(), eq(10))).thenReturn(List.of(event));

        executor.processBatch(10);

        verify(outboxEventRepository).markTerminalFailure(
                eq(event.getEventId()),
                eq("invalid_outbox_payload"),
                org.mockito.ArgumentMatchers.contains("Invalid payload for CaptureOutboxPayload"),
                eq(Instant.parse("2026-05-09T12:00:00Z")));
        verify(outboxEventRepository, never()).markRetryableFailure(any(), any(), any(), any(), any());
        verify(bankClient, never()).capture(any(), any());
    }
}
