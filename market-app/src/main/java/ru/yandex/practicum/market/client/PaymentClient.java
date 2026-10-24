package ru.yandex.practicum.market.client;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.client.generated.dto.BalanceResponse;
import ru.yandex.practicum.market.client.generated.dto.PaymentRequest;
import ru.yandex.practicum.market.client.generated.dto.PaymentResponse;

public interface PaymentClient {

    Mono<BalanceResponse> getBalance();

    Mono<PaymentResponse> pay(PaymentRequest request);
}