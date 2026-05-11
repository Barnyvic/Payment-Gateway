package com.paymentgateway.payments.infrastructure.persistence.paymentreceipt;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentgateway.AbstractPostgresIntegrationTest;
import com.paymentgateway.gateway.PaymentGatewayApplication;
import com.paymentgateway.payments.domain.model.Payment;
import com.paymentgateway.payments.domain.model.PaymentState;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.Money;
import com.paymentgateway.payments.domain.value.OrderId;
import com.paymentgateway.payments.domain.value.PaymentRef;
import com.paymentgateway.payments.domain.value.SupportedCurrency;
import com.paymentgateway.payments.infrastructure.persistence.entity.PaymentReceiptEntity;
import com.paymentgateway.payments.infrastructure.persistence.repository.PaymentReceiptJpaRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = PaymentGatewayApplication.class)
@Transactional
class PaymentReceiptRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PaymentReceiptRepository repository;

    @Autowired
    private PaymentReceiptJpaRepository jpaRepository;

    @Test
    void saveAndFind_roundTripPending() {
        PaymentRef ref = new PaymentRef(UUID.randomUUID());
        Payment pending = Payment.pending(
                ref, new OrderId("ord-42"), new CustomerId("cust-9"), new Money(5_000L, SupportedCurrency.USD));

        Payment saved = repository.save(pending);

        assertThat(saved.getState()).isEqualTo(PaymentState.PENDING);
        assertThat(repository.findByPaymentRef(ref)).hasValueSatisfying(p -> {
            assertThat(p.getOrderId().value()).isEqualTo("ord-42");
            assertThat(p.getCustomerId().value()).isEqualTo("cust-9");
            assertThat(p.getMoney().amountMinorUnits()).isEqualTo(5_000L);
        });
    }

    @Test
    void saveAfterAuthorize_setsAuthorizedAt() {
        PaymentRef ref = new PaymentRef(UUID.randomUUID());
        Payment payment =
                Payment.pending(ref, new OrderId("o1"), new CustomerId("c1"), new Money(100L, SupportedCurrency.USD));
        repository.save(payment);

        Payment loaded = repository.findByPaymentRef(ref).orElseThrow();
        loaded.authorize();
        repository.save(loaded);

        PaymentReceiptEntity row = jpaRepository.findById(ref.value()).orElseThrow();
        assertThat(row.getState()).isEqualTo(PaymentState.AUTHORIZED);
        assertThat(row.getAuthorizedAt()).isNotNull();
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isAfterOrEqualTo(row.getCreatedAt());
    }

    @Test
    void findByCustomerId_returnsAllPaymentsForCustomer() {
        CustomerId customerId = new CustomerId("cust-shared");
        PaymentRef r1 = new PaymentRef(UUID.randomUUID());
        PaymentRef r2 = new PaymentRef(UUID.randomUUID());
        repository.save(
                Payment.pending(r1, new OrderId("o1"), customerId, new Money(10L, SupportedCurrency.USD)));
        repository.save(
                Payment.pending(r2, new OrderId("o2"), customerId, new Money(20L, SupportedCurrency.USD)));

        assertThat(repository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .hasSize(2)
                .extracting(p -> p.getPaymentRef().value())
                .containsExactlyInAnyOrder(r1.value(), r2.value());
    }

    @Test
    void findByOrderId_returnsAllPaymentsForOrder() {
        OrderId orderId = new OrderId("shared-order");
        PaymentRef r1 = new PaymentRef(UUID.randomUUID());
        PaymentRef r2 = new PaymentRef(UUID.randomUUID());
        repository.save(
                Payment.pending(r1, orderId, new CustomerId("c1"), new Money(10L, SupportedCurrency.USD)));
        repository.save(
                Payment.pending(r2, orderId, new CustomerId("c1"), new Money(20L, SupportedCurrency.USD)));

        assertThat(repository.findByOrderIdOrderByCreatedAtDesc(orderId))
                .hasSize(2)
                .extracting(p -> p.getPaymentRef().value())
                .containsExactlyInAnyOrder(r1.value(), r2.value());
    }

    @Test
    void findReceiptRecordsByOrderId_returnsFullProjection() {
        OrderId orderId = new OrderId("order-read-projection");
        PaymentRef ref = new PaymentRef(UUID.randomUUID());
        repository.save(
                Payment.pending(ref, orderId, new CustomerId("c-read"), new Money(99L, SupportedCurrency.USD)));

        PaymentReceiptEntity row = jpaRepository.findById(ref.value()).orElseThrow();
        row.setBankAuthorizationId("bank-auth-1");
        row.setLastErrorCode("none");
        jpaRepository.saveAndFlush(row);

        assertThat(repository.findReceiptRecordsByOrderId(orderId))
                .singleElement()
                .satisfies((PaymentReceiptRecord rec) -> {
                    assertThat(rec.paymentRef()).isEqualTo(ref.value());
                    assertThat(rec.orderId()).isEqualTo("order-read-projection");
                    assertThat(rec.bankAuthorizationId()).isEqualTo("bank-auth-1");
                    assertThat(rec.lastErrorCode()).isEqualTo("none");
                    assertThat(rec.createdAt()).isNotNull();
                });
    }
}
