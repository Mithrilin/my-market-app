package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.model.Item;
import ru.yandex.practicum.market.model.Order;
import ru.yandex.practicum.market.model.OrderItem;
import ru.yandex.practicum.market.repository.ItemRepository;
import ru.yandex.practicum.market.repository.OrderItemRepository;
import ru.yandex.practicum.market.repository.OrderRepository;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;

    @Transactional
    @CacheEvict(cacheNames = {"item", "itemList"}, allEntries = true)
    public Mono<Order> saveNewOrder(List<Item> items, long totalSum) {
        Order order = Order.builder().totalSum(totalSum).build();

        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    if (items.isEmpty()) {
                        savedOrder.setOrderItemList(List.of());
                        return Mono.just(savedOrder);
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
                                items.forEach(i -> i.setCount(0));
                                return itemRepository.saveAll(items).then(Mono.just(savedOrder));
                            });
                });
    }
}
