package com.paymentgateway.payments.api.web;

import com.paymentgateway.payments.api.dto.ErrorResponse;
import com.paymentgateway.payments.application.exception.PaymentAuthorizationException;
import com.paymentgateway.payments.domain.exception.IdempotencyInProgressException;
import com.paymentgateway.payments.domain.exception.InvalidPaymentTransitionException;
import com.paymentgateway.payments.domain.exception.NoReceiptsForOrderException;
import com.paymentgateway.payments.domain.exception.PaymentAwaitingBankException;
import com.paymentgateway.payments.domain.exception.PaymentNotFoundException;
import com.paymentgateway.payments.domain.idempotency.exception.IdempotencyConflictException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.paymentgateway.payments.api")
public class PaymentApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentApiExceptionHandler.class);

    @ExceptionHandler(NoReceiptsForOrderException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoReceiptsForOrderException ex) {
        log.warn("No receipts for order: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NO_RECEIPTS_FOR_ORDER", ex.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        log.warn("Payment not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PAYMENT_NOT_FOUND", ex.getMessage()));
    }

    private static final String IDEMPOTENCY_KEY_MESSAGE = "Idempotency-Key header is required for this operation";

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())) {
            log.warn("Missing Idempotency-Key header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("MISSING_IDEMPOTENCY_KEY", IDEMPOTENCY_KEY_MESSAGE));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MISSING_HEADER", ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        boolean idempotencyKeyViolation = ex.getConstraintViolations().stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().contains("idempotencyKey"));
        if (idempotencyKeyViolation) {
            log.warn("Blank Idempotency-Key header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("MISSING_IDEMPOTENCY_KEY", IDEMPOTENCY_KEY_MESSAGE));
        }
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Request validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("IDEMPOTENCY_KEY_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyInProgress(IdempotencyInProgressException ex) {
        log.warn("Idempotency in progress: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("IDEMPOTENCY_IN_PROGRESS", ex.getMessage()));
    }

    @ExceptionHandler(PaymentAuthorizationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAuthorization(PaymentAuthorizationException ex) {
        if (ex.isTransientFailure()) {
            log.warn("Bank authorization unavailable: {} ({})", ex.getMessage(), ex.getErrorCode());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("BANK_AUTHORIZATION_UNAVAILABLE", ex.getMessage()));
        }
        log.warn("Bank authorization declined: {} ({})", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("BANK_AUTHORIZATION_DECLINED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidPaymentTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidPaymentTransitionException ex) {
        log.warn(
                "Invalid payment transition: {} in state {}",
                ex.getAttemptedAction(),
                ex.getCurrentState());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        "INVALID_PAYMENT_TRANSITION",
                        "Cannot execute " + ex.getAttemptedAction() + " in state " + ex.getCurrentState()));
    }

    @ExceptionHandler(PaymentAwaitingBankException.class)
    public ResponseEntity<ErrorResponse> handleAwaitingBank(PaymentAwaitingBankException ex) {
        log.warn("Payment awaiting bank: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("PAYMENT_AWAITING_BANK", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request validation failed";
        }
        log.warn("Request body validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
