package com.paymentgateway.payments.infrastructure.jobs.outbox;

import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;

public interface OutboxBankCompletionPort {

    void recordCapture(PaymentRef paymentRef, BankCaptureResponse response);

    void recordVoid(PaymentRef paymentRef, BankVoidResponse response);

    void recordRefund(PaymentRef paymentRef, BankRefundResponse response);
}
