package com.paymentgateway.payments.api.dto;

import com.paymentgateway.payments.application.PaymentCommandService.AuthorizePaymentCommand;
import com.paymentgateway.payments.application.PaymentCommandService.CardPayload;

public record AuthorizePaymentHttpRequest(String orderId, String customerId, CardHttp card, long amountCents) {

    public AuthorizePaymentCommand toCommand() {
        if (card == null) {
            throw new IllegalArgumentException("card is required");
        }
        return new AuthorizePaymentCommand(
                orderId,
                customerId,
                new CardPayload(card.number(), card.cvv(), card.expiryMonth(), card.expiryYear()),
                amountCents);
    }

    public record CardHttp(String number, String cvv, String expiryMonth, String expiryYear) {}
}
