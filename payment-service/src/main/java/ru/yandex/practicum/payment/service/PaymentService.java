package ru.yandex.practicum.payment.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.generated.BalanceResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentResponse;

public interface PaymentService {

    Mono<BalanceResponse> getBalance(Long userId);

    Mono<PaymentResponse> pay(PaymentRequest request);
}