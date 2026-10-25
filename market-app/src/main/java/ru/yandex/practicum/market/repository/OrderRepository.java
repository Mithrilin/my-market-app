package ru.yandex.practicum.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.yandex.practicum.market.model.Order;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
