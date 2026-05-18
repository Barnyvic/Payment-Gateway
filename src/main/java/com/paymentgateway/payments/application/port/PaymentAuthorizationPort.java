package com.paymentgateway.payments.application.port;

import com.paymentgateway.payments.domain.value.SupportedCurrency;

public interface PaymentAuthorizationPort {

    AuthorizeResult authorize(AuthorizeBankCommand command, String bankIdempotencyKey);

    record AuthorizeBankCommand(
            String cardNumber,
            String cvv,
            String expiryMonth,
            String expiryYear,
            long amountCents,
            SupportedCurrency currency) {}

    record AuthorizeResult(String authorizationId, String status) {}
}
