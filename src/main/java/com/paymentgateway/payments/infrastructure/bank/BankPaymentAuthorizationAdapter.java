package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.application.exception.PaymentAuthorizationException;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort.AuthorizeBankCommand;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort.AuthorizeResult;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import org.springframework.stereotype.Component;

@Component
public class BankPaymentAuthorizationAdapter implements PaymentAuthorizationPort {

    private final BankClient bankClient;

    public BankPaymentAuthorizationAdapter(BankClient bankClient) {
        this.bankClient = bankClient;
    }

    @Override
    public AuthorizeResult authorize(AuthorizeBankCommand command, String bankIdempotencyKey) {
        try {
            var response = bankClient.authorize(
                    new BankAuthorizeRequest(
                            command.cardNumber(),
                            command.cvv(),
                            command.expiryMonth(),
                            command.expiryYear(),
                            command.amountCents(),
                            command.currency().name()),
                    bankIdempotencyKey);
            return new AuthorizeResult(response.authorizationId(), response.status());
        } catch (BankClientException ex) {
            boolean transientFailure = ex.getDetails().category() == BankErrorCategory.TRANSIENT;
            throw new PaymentAuthorizationException(
                    ex.getDetails().code(), ex.getDetails().message(), transientFailure);
        }
    }
}
