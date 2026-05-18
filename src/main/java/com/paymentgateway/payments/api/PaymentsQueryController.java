package com.paymentgateway.payments.api;

import com.paymentgateway.payments.api.dto.PaymentsByCustomerResponse;
import com.paymentgateway.payments.api.dto.PaymentsByOrderResponse;
import com.paymentgateway.payments.application.PaymentsQueryService;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentsQueryController {

    private static final Logger log = LoggerFactory.getLogger(PaymentsQueryController.class);

    private final PaymentsQueryService paymentsQueryService;

    public PaymentsQueryController(PaymentsQueryService paymentsQueryService) {
        this.paymentsQueryService = paymentsQueryService;
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentsByOrderResponse> listByOrder(@PathVariable("orderId") String orderId) {
        String normalized = orderId == null ? "" : orderId.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("orderId must be non-blank");
        }
        OrderId id = new OrderId(normalized);
        log.debug("GET /by-order/{}", id.value());
        return ResponseEntity.ok(PaymentsByOrderResponse.of(id.value(), paymentsQueryService.listByOrder(id)));
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
        log.debug("GET /by-customer/{} limit={}", id.value(), limit);
        return ResponseEntity.ok(
                PaymentsByCustomerResponse.of(id.value(), paymentsQueryService.listByCustomer(id, limit)));
    }
}
