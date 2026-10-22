package ru.yandex.practicum.market.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Rendering> handleNotFoundException(NotFoundException e) {
        log.error(e.getLocalizedMessage());
        return Mono.just(Rendering.view("error")
                .modelAttribute("errorMessage", e.getMessage())
                .modelAttribute("errorStatus", 404)
                .build());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleBadRequestException(BadRequestException e) {
        log.error(e.getLocalizedMessage());
        return Mono.just(Rendering.view("error")
                .modelAttribute("errorMessage", e.getMessage())
                .modelAttribute("errorStatus", 400)
                .build());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleValidationException(WebExchangeBindException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
                .map(oe -> oe.getObjectName() + " - " + oe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error(msg);
        return Mono.just(Rendering.view("error")
                .modelAttribute("errorMessage", msg)
                .modelAttribute("errorStatus", 400)
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleConstraintException(ConstraintViolationException e) {
        log.error(e.getLocalizedMessage());
        return Mono.just(Rendering.view("error")
                .modelAttribute("errorMessage", e.getMessage())
                .modelAttribute("errorStatus", 400)
                .build());
    }

    @ExceptionHandler({
            MissingRequestValueException.class,
            ServerWebInputException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleMissingOrBadInput(Exception e) {
        log.error("Bad request: {}", e.getMessage());
        return Mono.just(Rendering.view("error")
                .modelAttribute("errorMessage", e.getMessage())
                .modelAttribute("errorStatus", HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Rendering> handleAll(Exception e) {
        log.error("Unhandled error", e);
        return Mono.just(Rendering.view("error")
                .modelAttribute("errorMessage", e.getMessage())
                .modelAttribute("errorStatus", 500)
                .build());
    }
}
