package com.paymentgateway.payments.infrastructure.bank;

import com.paymentgateway.payments.application.exception.PaymentAuthorizationException;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort.AuthorizeBankCommand;
import com.paymentgateway.payments.application.port.PaymentAuthorizationPort.AuthorizeResult;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeRequest;
import com.paymentgateway.payments.infrastructure.bank.model.BankClientException;
import com.paymentgateway.payments.infrastructure.bank.model.BankErrorCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BankPaymentAuthorizationAdapter implements PaymentAuthorizationPort {

    private static final Logger log = LoggerFactory.getLogger(BankPaymentAuthorizationAdapter.class);

    private final BankClient bankClient;

    public BankPaymentAuthorizationAdapter(BankClient bankClient) {
        this.bankClient = bankClient;
    }

    @Override
    public AuthorizeResult authorize(AuthorizeBankCommand command, String bankIdempotencyKey) {
        log.debug("Calling bank authorize idempotencyKey={} amountCents={}", bankIdempotencyKey, command.amountCents());
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
            log.debug(
                    "Bank authorize succeeded idempotencyKey={} authorizationId={}",
                    bankIdempotencyKey,
                    response.authorizationId());
            return new AuthorizeResult(response.authorizationId(), response.status());
        } catch (BankClientException ex) {
            boolean transientFailure = ex.getDetails().category() == BankErrorCategory.TRANSIENT;
            log.warn(
                    "Bank authorize failed idempotencyKey={} code={} transient={}",
                    bankIdempotencyKey,
                    ex.getDetails().code(),
                    transientFailure);
            throw new PaymentAuthorizationException(
                    ex.getDetails().code(), ex.getDetails().message(), transientFailure);
        }
    }
}
