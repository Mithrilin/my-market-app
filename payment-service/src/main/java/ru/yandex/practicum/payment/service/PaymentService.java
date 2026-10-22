package ru.yandex.practicum.payment.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentRequest;
import ru.yandex.practicum.payment.dto.PaymentResponse;

public interface PaymentService {

    Mono<BalanceResponse> getBalance();

    Mono<PaymentResponse> pay(PaymentRequest request);
}