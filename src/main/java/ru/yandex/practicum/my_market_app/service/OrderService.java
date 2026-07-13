package ru.yandex.practicum.my_market_app.service;

import ru.yandex.practicum.my_market_app.dto.OrderDto;

import java.util.List;

public interface OrderService {

    List<OrderDto> getOrderList();

    OrderDto getOrderById(Long id);

    OrderDto createOrder();
}
