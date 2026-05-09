package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;

public interface BankClient {
    BankAuthorizeResponse authorize(BankAuthorizeRequest request, String idempotencyKey);

    BankCaptureResponse capture(BankCaptureRequest request, String idempotencyKey);

    BankVoidResponse voidAuthorization(BankVoidRequest request, String idempotencyKey);

    BankRefundResponse refund(BankRefundRequest request, String idempotencyKey);
}
