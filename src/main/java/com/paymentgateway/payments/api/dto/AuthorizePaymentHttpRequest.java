package com.paymentgateway.payments.api.dto;

import com.paymentgateway.payments.application.PaymentCommandService.AuthorizePaymentCommand;
import com.paymentgateway.payments.application.PaymentCommandService.CardPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AuthorizePaymentHttpRequest(
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @Valid CardHttp card,
        @Positive long amountCents) {

    public AuthorizePaymentCommand toCommand() {
        return new AuthorizePaymentCommand(
                orderId,
                customerId,
                new CardPayload(card.number(), card.cvv(), card.expiryMonth(), card.expiryYear()),
                amountCents);
    }

    public record CardHttp(
            @NotBlank String number,
            @NotBlank String cvv,
            @NotBlank String expiryMonth,
            @NotBlank String expiryYear) {}
}
