package com.paymentgateway.payments.api.web;

import com.paymentgateway.payments.api.dto.ErrorResponse;
import com.paymentgateway.payments.domain.exception.IdempotencyInProgressException;
import com.paymentgateway.payments.domain.exception.InvalidPaymentTransitionException;
import com.paymentgateway.payments.domain.exception.MissingIdempotencyKeyException;
import com.paymentgateway.payments.domain.exception.NoReceiptsForOrderException;
import com.paymentgateway.payments.domain.exception.PaymentAwaitingBankException;
import com.paymentgateway.payments.domain.exception.PaymentNotFoundException;
import com.paymentgateway.payments.domain.idempotency.exception.IdempotencyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.paymentgateway.payments.api")
public class PaymentApiExceptionHandler {

    @ExceptionHandler(NoReceiptsForOrderException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoReceiptsForOrderException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NO_RECEIPTS_FOR_ORDER", ex.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PAYMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleMissingIdempotencyKey(MissingIdempotencyKeyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MISSING_IDEMPOTENCY_KEY", ex.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("MISSING_IDEMPOTENCY_KEY", ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MISSING_HEADER", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("IDEMPOTENCY_KEY_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyInProgress(IdempotencyInProgressException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("IDEMPOTENCY_IN_PROGRESS", ex.getMessage()));
    }

    @ExceptionHandler(InvalidPaymentTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidPaymentTransitionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        "INVALID_PAYMENT_TRANSITION",
                        "Cannot execute " + ex.getAttemptedCommand() + " in state " + ex.getCurrentState()));
    }

    @ExceptionHandler(PaymentAwaitingBankException.class)
    public ResponseEntity<ErrorResponse> handleAwaitingBank(PaymentAwaitingBankException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("PAYMENT_AWAITING_BANK", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
