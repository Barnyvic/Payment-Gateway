package com.paymentgateway.payments.infrastructure.jobs.outbox;

import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOutboxBankCompletionService implements OutboxBankCompletionPort {

    private final PaymentReceiptRepository paymentReceiptRepository;

    public PaymentOutboxBankCompletionService(PaymentReceiptRepository paymentReceiptRepository) {
        this.paymentReceiptRepository = paymentReceiptRepository;
    }

    @Override
    @Transactional
    public void recordAuthorize(PaymentRef paymentRef, BankAuthorizeResponse response) {
        paymentReceiptRepository.recordAuthorizeSuccess(paymentRef, response.authorizationId());
    }

    @Override
    @Transactional
    public void recordCapture(PaymentRef paymentRef, BankCaptureResponse response) {
        paymentReceiptRepository.recordCaptureSuccess(paymentRef, response.captureId());
    }

    @Override
    @Transactional
    public void recordVoid(PaymentRef paymentRef, BankVoidResponse response) {
        paymentReceiptRepository.recordVoidSuccess(paymentRef, response.voidId());
    }

    @Override
    @Transactional
    public void recordRefund(PaymentRef paymentRef, BankRefundResponse response) {
        paymentReceiptRepository.recordRefundSuccess(paymentRef, response.refundId());
    }
}
