package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;

public interface BankClient {
    BankAuthorizeResponse authorize(BankAuthorizeRequest request, String idempotencyKey);
}
