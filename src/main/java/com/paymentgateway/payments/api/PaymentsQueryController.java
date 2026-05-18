package com.paymentgateway.payments.api;

import com.paymentgateway.payments.api.dto.PaymentsByCustomerResponse;
import com.paymentgateway.payments.api.dto.PaymentsByOrderResponse;
import com.paymentgateway.payments.application.ListPaymentsByCustomerService;
import com.paymentgateway.payments.application.ListPaymentsByOrderService;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.OrderId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentsQueryController {

    private final ListPaymentsByOrderService listPaymentsByOrderService;
    private final ListPaymentsByCustomerService listPaymentsByCustomerService;

    public PaymentsQueryController(
            ListPaymentsByOrderService listPaymentsByOrderService,
            ListPaymentsByCustomerService listPaymentsByCustomerService) {
        this.listPaymentsByOrderService = listPaymentsByOrderService;
        this.listPaymentsByCustomerService = listPaymentsByCustomerService;
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentsByOrderResponse> listByOrder(@PathVariable("orderId") String orderId) {
        String normalized = orderId == null ? "" : orderId.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("orderId must be non-blank");
        }
        OrderId id = new OrderId(normalized);
        return ResponseEntity.ok(PaymentsByOrderResponse.of(id.value(), listPaymentsByOrderService.listByOrder(id)));
    }

    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<PaymentsByCustomerResponse> listByCustomer(
            @PathVariable("customerId") String customerId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        String normalized = customerId == null ? "" : customerId.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("customerId must be non-blank");
        }
        CustomerId id = new CustomerId(normalized);
        return ResponseEntity.ok(
                PaymentsByCustomerResponse.of(id.value(), listPaymentsByCustomerService.listByCustomer(id, limit)));
    }
}
