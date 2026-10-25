package ru.yandex.practicum.market.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.OrderDto;

public interface OrderService {

    Flux<OrderDto> getOrderList();

    Mono<OrderDto> getOrderById(Long id);

    Mono<OrderDto> createOrder();
}
