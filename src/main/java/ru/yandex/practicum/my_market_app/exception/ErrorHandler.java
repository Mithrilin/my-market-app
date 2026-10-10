package ru.yandex.practicum.my_market_app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(Model model, NotFoundException e) {
        model.addAttribute("errorMessage", e.getLocalizedMessage());
        model.addAttribute("errorStatus", HttpStatus.NOT_FOUND.value());
        log.error(e.getLocalizedMessage());
        return "error";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequestException(Model model, BadRequestException e) {
        model.addAttribute("errorMessage", e.getLocalizedMessage());
        model.addAttribute("errorStatus", HttpStatus.BAD_REQUEST.value());
        log.error(e.getLocalizedMessage());
        return "error";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMethodArgumentNotValid(Model model, MethodArgumentNotValidException e) {
        StringBuilder builder = new StringBuilder();
        for (ObjectError oe : e.getBindingResult().getAllErrors()) {
            builder.append(oe.getObjectName()).append(" - validation error. Message: ").append(oe.getDefaultMessage())
                    .append("; ");
            log.error("'{}' validation error. Message: {}.", oe.getObjectName(), oe.getDefaultMessage());
        }
        model.addAttribute("errorMessage", builder.toString());
        model.addAttribute("errorStatus", HttpStatus.BAD_REQUEST.value());
        return "error";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAllException(Model model, Exception e) {
        model.addAttribute("errorMessage", e.getLocalizedMessage());
        model.addAttribute("errorStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        log.error(e.getLocalizedMessage());
        return "error";
    }
}
