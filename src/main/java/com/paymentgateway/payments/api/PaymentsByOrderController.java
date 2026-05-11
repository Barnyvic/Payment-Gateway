package com.paymentgateway.payments.api;

import com.paymentgateway.payments.api.dto.PaymentsByOrderResponse;
import com.paymentgateway.payments.application.ListPaymentsByOrderService;
import com.paymentgateway.payments.domain.value.OrderId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentsByOrderController {

    private final ListPaymentsByOrderService listPaymentsByOrderService;

    public PaymentsByOrderController(ListPaymentsByOrderService listPaymentsByOrderService) {
        this.listPaymentsByOrderService = listPaymentsByOrderService;
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
}
