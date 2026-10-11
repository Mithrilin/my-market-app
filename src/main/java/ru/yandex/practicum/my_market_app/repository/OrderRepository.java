package ru.yandex.practicum.my_market_app.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.yandex.practicum.my_market_app.model.Order;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
