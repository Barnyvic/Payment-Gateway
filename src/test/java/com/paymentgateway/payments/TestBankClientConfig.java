package com.paymentgateway.payments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentgateway.payments.infrastructure.bank.BankClient;
import com.paymentgateway.payments.infrastructure.bank.model.BankAuthorizeResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankCaptureResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankRefundResponse;
import com.paymentgateway.payments.infrastructure.bank.model.BankVoidResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestBankClientConfig {

    @Bean
    @Primary
    BankClient testBankClient() {
        BankClient bankClient = mock(BankClient.class);
        when(bankClient.authorize(any(), anyString()))
                .thenReturn(new BankAuthorizeResponse("bank-auth-test", "AUTHORIZED"));
        when(bankClient.capture(any(), anyString())).thenReturn(new BankCaptureResponse("bank-cap-test", "CAPTURED"));
        when(bankClient.voidAuthorization(any(), anyString())).thenReturn(new BankVoidResponse("bank-void-test", "VOIDED"));
        when(bankClient.refund(any(), anyString())).thenReturn(new BankRefundResponse("bank-refund-test", "REFUNDED"));
        return bankClient;
    }
}
