package com.paymentgateway.payments.infrastructure.jobs.outbox;

import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOutboxBankCompletionService implements OutboxBankCompletionPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxBankCompletionService.class);

    private final PaymentReceiptRepository paymentReceiptRepository;

    public PaymentOutboxBankCompletionService(PaymentReceiptRepository paymentReceiptRepository) {
        this.paymentReceiptRepository = paymentReceiptRepository;
    }

    @Override
    @Transactional
    public void recordCapture(PaymentRef paymentRef, BankCaptureResponse response) {
        paymentReceiptRepository.recordCaptureSuccess(paymentRef, response.captureId());
        log.info("Recorded capture paymentRef={} bankCaptureId={}", paymentRef.value(), response.captureId());
    }

    @Override
    @Transactional
    public void recordVoid(PaymentRef paymentRef, BankVoidResponse response) {
        paymentReceiptRepository.recordVoidSuccess(paymentRef, response.voidId());
        log.info("Recorded void paymentRef={} bankVoidId={}", paymentRef.value(), response.voidId());
    }

    @Override
    @Transactional
    public void recordRefund(PaymentRef paymentRef, BankRefundResponse response) {
        paymentReceiptRepository.recordRefundSuccess(paymentRef, response.refundId());
        log.info("Recorded refund paymentRef={} bankRefundId={}", paymentRef.value(), response.refundId());
    }
}
