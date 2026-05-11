package com.paymentgateway.payments.api.web;

import com.paymentgateway.payments.api.dto.ErrorResponse;
import com.paymentgateway.payments.domain.exception.NoReceiptsForOrderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.paymentgateway.payments.api")
public class PaymentApiExceptionHandler {

    @ExceptionHandler(NoReceiptsForOrderException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoReceiptsForOrderException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NO_RECEIPTS_FOR_ORDER", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
