package ru.yandex.practicum.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.generated.PaymentErrorResponse;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(org.springframework.web.reactive.resource.NoResourceFoundException.class)
    public Mono<ResponseEntity<Void>> handleNoResource() {
        return Mono.just(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public Mono<ResponseEntity<PaymentErrorResponse>> handleInsufficientFunds(InsufficientFundsException e) {
        log.error(e.getLocalizedMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PaymentErrorResponse().errorMessage(e.getMessage())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<PaymentErrorResponse>> handleValidationException(WebExchangeBindException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
                .map(oe -> oe.getObjectName() + " - " + oe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error(msg);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PaymentErrorResponse().errorMessage(msg)));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<PaymentErrorResponse>> handleAll(Exception e) {
        log.error("Unhandled error", e);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new PaymentErrorResponse().errorMessage(e.getMessage())));
    }
}