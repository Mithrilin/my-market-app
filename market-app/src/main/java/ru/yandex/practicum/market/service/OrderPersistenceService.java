package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.model.ItemWithCount;
import ru.yandex.practicum.market.model.Order;
import ru.yandex.practicum.market.model.OrderItem;
import ru.yandex.practicum.market.repository.CartItemRepository;
import ru.yandex.practicum.market.repository.OrderItemRepository;
import ru.yandex.practicum.market.repository.OrderRepository;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    @CacheEvict(cacheNames = {"item", "itemList"}, allEntries = true)
    public Mono<Order> saveNewOrder(List<ItemWithCount> items, long totalSum, Long userId) {
        Order order = Order.builder().userId(userId).totalSum(totalSum).build();

        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    if (items.isEmpty()) {
                        savedOrder.setOrderItemList(List.of());
                        return cartItemRepository.deleteAllByUserId(userId)
                                .then(Mono.just(savedOrder));
                    }

                    List<OrderItem> orderItems = items.stream()
                            .map(item -> OrderItem.builder()
                                    .orderId(savedOrder.getId())
                                    .title(item.getTitle())
                                    .price(item.getPrice())
                                    .count(item.getCount())
                                    .build())
                            .toList();

                    return orderItemRepository.saveAll(orderItems)
                            .collectList()
                            .flatMap(savedItems -> {
                                savedOrder.setOrderItemList(savedItems);
                                return cartItemRepository.deleteAllByUserId(userId)
                                        .then(Mono.just(savedOrder));
                            });
                });
    }
}