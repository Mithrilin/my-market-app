package ru.yandex.practicum.payment.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentRequest;
import ru.yandex.practicum.payment.dto.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/balance")
    public Mono<BalanceResponse> getBalance() {
        log.info("Getting current balance.");
        return paymentService.getBalance();
    }

    @PostMapping
    public Mono<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        log.info("Processing payment for order {} with amount {}.", request.orderId(), request.amount());
        return paymentService.pay(request);
    }
}