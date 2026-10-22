package ru.yandex.practicum.market.client;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.client.dto.BalanceResponse;
import ru.yandex.practicum.market.client.dto.PaymentRequest;
import ru.yandex.practicum.market.client.dto.PaymentResponse;

public interface PaymentClient {

    Mono<BalanceResponse> getBalance();

    Mono<PaymentResponse> pay(PaymentRequest request);
}