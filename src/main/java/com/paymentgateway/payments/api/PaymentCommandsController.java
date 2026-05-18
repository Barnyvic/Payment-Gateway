package com.paymentgateway.payments.api;

import com.paymentgateway.payments.api.dto.AuthorizePaymentHttpRequest;
import com.paymentgateway.payments.application.PaymentCommandDispatchResult;
import com.paymentgateway.payments.application.PaymentCommandService;
import com.paymentgateway.payments.domain.value.PaymentRef;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentCommandsController {

    private final PaymentCommandService paymentCommandService;

    public PaymentCommandsController(PaymentCommandService paymentCommandService) {
        this.paymentCommandService = paymentCommandService;
    }

    @PostMapping(value = "/authorize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> authorize(
            @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody AuthorizePaymentHttpRequest body) {
        PaymentCommandDispatchResult result = paymentCommandService.authorize(idempotencyKey, body.toCommand());
        return toHttpResponse(result);
    }

    @PostMapping("/{paymentRef}/capture")
    public ResponseEntity<String> capture(
            @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @PathVariable("paymentRef") String paymentRefRaw) {
        PaymentRef paymentRef = parsePaymentRef(paymentRefRaw);
        PaymentCommandDispatchResult result = paymentCommandService.capture(idempotencyKey, paymentRef);
        return toHttpResponse(result);
    }

    @PostMapping("/{paymentRef}/void")
    public ResponseEntity<String> voidPayment(
            @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @PathVariable("paymentRef") String paymentRefRaw) {
        PaymentRef paymentRef = parsePaymentRef(paymentRefRaw);
        PaymentCommandDispatchResult result = paymentCommandService.voidPayment(idempotencyKey, paymentRef);
        return toHttpResponse(result);
    }

    @PostMapping("/{paymentRef}/refund")
    public ResponseEntity<String> refund(
            @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @PathVariable("paymentRef") String paymentRefRaw) {
        PaymentRef paymentRef = parsePaymentRef(paymentRefRaw);
        PaymentCommandDispatchResult result = paymentCommandService.refund(idempotencyKey, paymentRef);
        return toHttpResponse(result);
    }

    private static PaymentRef parsePaymentRef(String raw) {
        try {
            return new PaymentRef(UUID.fromString(raw.strip()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("paymentRef must be a valid UUID");
        }
    }

    private static ResponseEntity<String> toHttpResponse(PaymentCommandDispatchResult result) {
        if (result.type() == PaymentCommandDispatchResult.Type.REPLAYED_200) {
            return ResponseEntity.ok()
                    .header(PaymentHttpConstants.X_IDEMPOTENT_REPLAYED, "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.responseBodyJson());
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.responseBodyJson());
    }
}
