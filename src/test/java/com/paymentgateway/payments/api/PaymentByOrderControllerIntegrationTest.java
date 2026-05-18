package com.paymentgateway.payments.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paymentgateway.AbstractPostgresIntegrationTest;
import com.paymentgateway.gateway.PaymentGatewayApplication;
import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = PaymentGatewayApplication.class)
@AutoConfigureMockMvc
@Transactional
class PaymentByOrderControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentReceiptRepository paymentReceiptRepository;

    @Test
    void listByOrder_returns404WhenNoReceipts() throws Exception {
        mockMvc.perform(get("/v1/payments/by-order/unknown-order-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RECEIPTS_FOR_ORDER"));
    }

    @Test
    void listByOrder_returns400WhenOrderIdBlank() throws Exception {
        mockMvc.perform(get("/v1/payments/by-order/{orderId}", " \t\n "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void listByOrder_returnsReceiptsOrderedByCreatedAtDesc() throws Exception {
        OrderId orderId = new OrderId("order-api-1");
        PaymentRef r1 = new PaymentRef(UUID.randomUUID());
        PaymentRef r2 = new PaymentRef(UUID.randomUUID());
        paymentReceiptRepository.save(
                Payment.pending(r1, orderId, new CustomerId("cust-1"), new Money(100L, SupportedCurrency.USD)));
        paymentReceiptRepository.save(
                Payment.pending(r2, orderId, new CustomerId("cust-1"), new Money(200L, SupportedCurrency.USD)));

        mockMvc.perform(get("/v1/payments/by-order/order-api-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-api-1"))
                .andExpect(jsonPath("$.payments.length()").value(2))
                .andExpect(jsonPath("$.payments[0].amountCents").exists())
                .andExpect(jsonPath("$.payments[0].state").value("PENDING"))
                .andExpect(jsonPath("$.payments[0].currency").value("USD"));
    }

    @Test
    void listByCustomer_returnsEmptyArrayWhenNoReceipts() throws Exception {
        mockMvc.perform(get("/v1/payments/by-customer/unknown-customer-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("unknown-customer-999"))
                .andExpect(jsonPath("$.payments.length()").value(0));
    }

    @Test
    void listByCustomer_returnsReceiptsWithLimit() throws Exception {
        CustomerId customerId = new CustomerId("cust-query-1");
        paymentReceiptRepository.save(
                Payment.pending(
                        new PaymentRef(UUID.randomUUID()),
                        new OrderId("o-a"),
                        customerId,
                        new Money(10L, SupportedCurrency.USD)));
        paymentReceiptRepository.save(
                Payment.pending(
                        new PaymentRef(UUID.randomUUID()),
                        new OrderId("o-b"),
                        customerId,
                        new Money(20L, SupportedCurrency.USD)));

        mockMvc.perform(get("/v1/payments/by-customer/cust-query-1").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-query-1"))
                .andExpect(jsonPath("$.payments.length()").value(1));
    }
}
