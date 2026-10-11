package ru.yandex.practicum.my_market_app.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.dto.OrderDto;

public interface OrderService {

    Flux<OrderDto> getOrderList();

    Mono<OrderDto> getOrderById(Long id);

    Mono<OrderDto> createOrder();
}
