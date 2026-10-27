package ru.yandex.practicum.market.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.OrderDto;

public interface OrderService {

    Flux<OrderDto> getOrderList(Long userId);

    Mono<OrderDto> getOrderById(Long id, Long userId);

    Mono<OrderDto> createOrder(Long userId);
}