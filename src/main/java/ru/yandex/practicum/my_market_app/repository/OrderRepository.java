package ru.yandex.practicum.my_market_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.my_market_app.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
