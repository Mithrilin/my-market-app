package ru.yandex.practicum.my_market_app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", e.getLocalizedMessage());
        error.put("stackTrace", getStackTraceAsString(e));
        ErrorResponse errorResponse = new ErrorResponse(error, HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        log.error(e.getLocalizedMessage());
        return errorResponse;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (ObjectError oe : e.getBindingResult().getAllErrors()) {
            errors.put(oe.getObjectName(), oe.getDefaultMessage());
            log.error("'{}' validation error. Message: {}.", oe.getObjectName(), oe.getDefaultMessage());
        }
        for (FieldError re : e.getBindingResult().getFieldErrors()) {
            errors.put(re.getField(), re.getDefaultMessage());
            log.error("'{}' validation error. Message: {}.", re.getField(), re.getDefaultMessage());
        }
        return new ErrorResponse(errors, HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", e.getLocalizedMessage());
        error.put("stackTrace", getStackTraceAsString(e));
        ErrorResponse errorResponse =
                new ErrorResponse(error, HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now());
        log.error(e.getLocalizedMessage());
        return errorResponse;
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
