package com.paymentgateway.payments.api;

import com.paymentgateway.payments.api.dto.AuthorizePaymentHttpRequest;
import com.paymentgateway.payments.application.PaymentCommandService;
import com.paymentgateway.payments.application.PaymentCommandService.PaymentCommandDispatchResult;
import com.paymentgateway.payments.domain.value.PaymentRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/v1/payments")
public class PaymentCommandsController {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandsController.class);

    private final PaymentCommandService paymentCommandService;

    public PaymentCommandsController(PaymentCommandService paymentCommandService) {
        this.paymentCommandService = paymentCommandService;
    }

    @PostMapping(value = "/authorize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> authorize(
            @NotBlank @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody AuthorizePaymentHttpRequest body) {
        log.debug("POST /authorize orderId={} customerId={}", body.orderId(), body.customerId());
        PaymentCommandDispatchResult result =
                paymentCommandService.authorize(idempotencyKey.strip(), body.toCommand());
        return toHttpResponse("authorize", result);
    }

    @PostMapping("/{paymentRef}/capture")
    public ResponseEntity<String> capture(
            @NotBlank @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @PathVariable("paymentRef") String paymentRefRaw) {
        PaymentRef paymentRef = parsePaymentRef(paymentRefRaw);
        log.debug("POST /{}/capture", paymentRef.value());
        PaymentCommandDispatchResult result =
                paymentCommandService.capture(idempotencyKey.strip(), paymentRef);
        return toHttpResponse("capture", result);
    }

    @PostMapping("/{paymentRef}/void")
    public ResponseEntity<String> voidPayment(
            @NotBlank @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @PathVariable("paymentRef") String paymentRefRaw) {
        PaymentRef paymentRef = parsePaymentRef(paymentRefRaw);
        log.debug("POST /{}/void", paymentRef.value());
        PaymentCommandDispatchResult result =
                paymentCommandService.voidPayment(idempotencyKey.strip(), paymentRef);
        return toHttpResponse("void", result);
    }

    @PostMapping("/{paymentRef}/refund")
    public ResponseEntity<String> refund(
            @NotBlank @RequestHeader(value = PaymentHttpConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @PathVariable("paymentRef") String paymentRefRaw) {
        PaymentRef paymentRef = parsePaymentRef(paymentRefRaw);
        log.debug("POST /{}/refund", paymentRef.value());
        PaymentCommandDispatchResult result =
                paymentCommandService.refund(idempotencyKey.strip(), paymentRef);
        return toHttpResponse("refund", result);
    }

    private static PaymentRef parsePaymentRef(String raw) {
        try {
            return new PaymentRef(UUID.fromString(raw.strip()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("paymentRef must be a valid UUID");
        }
    }

    private static ResponseEntity<String> toHttpResponse(String operation, PaymentCommandDispatchResult result) {
        if (result.type() == PaymentCommandDispatchResult.Type.REPLAYED_200) {
            log.debug("{} idempotent replay -> 200", operation);
            return ResponseEntity.ok()
                    .header(PaymentHttpConstants.X_IDEMPOTENT_REPLAYED, "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.responseBodyJson());
        }
        log.debug("{} accepted -> 202", operation);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.responseBodyJson());
    }
}
