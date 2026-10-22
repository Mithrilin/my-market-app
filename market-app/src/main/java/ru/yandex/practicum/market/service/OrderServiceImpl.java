package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.OrderDto;
import ru.yandex.practicum.market.exception.NotFoundException;
import ru.yandex.practicum.market.mapper.OrderMapper;
import ru.yandex.practicum.market.model.Order;
import ru.yandex.practicum.market.model.OrderItem;
import ru.yandex.practicum.market.repository.ItemRepository;
import ru.yandex.practicum.market.repository.OrderItemRepository;
import ru.yandex.practicum.market.repository.OrderRepository;

import java.util.List;

import static ru.yandex.practicum.market.service.ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART;

@Slf4j
@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;

    private final OrderMapper orderMapper;

    @Override
    public Flux<OrderDto> getOrderList() {
        Flux<OrderDto> orderDtoFlux = orderRepository.findAll()
                .concatMap(this::buildOrderDto);
        log.info("Order List has been returned.");
        return orderDtoFlux;
    }

    @Override
    public Mono<OrderDto> getOrderById(Long id) {
        Mono<OrderDto> orderDtoMono = orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        String.format("Order with ID = %d not found", id))))
                .flatMap(this::buildOrderDto);
        log.info("Order with ID = {} returned.", id);
        return orderDtoMono;
    }

    @Override
    @Transactional
    public Mono<OrderDto> createOrder() {
        Mono<OrderDto> orderDtoMono = itemRepository.findByCountGreaterThanEqual(MIN_COUNT_ITEMS_IN_CART)
                .collectList()
                .flatMap(items -> {
                    long totalSum = items.stream()
                            .mapToLong(i -> i.getPrice() * i.getCount())
                            .sum();

                    Order order = Order.builder().totalSum(totalSum).build();

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
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
                                            // обнуляем корзину
                                            items.forEach(i -> i.setCount(0));
                                            return itemRepository.saveAll(items).then(Mono.just(savedOrder));
                                        });
                            });
                })
                .map(orderMapper::toOrderDto);
        log.info("Order created.");
        return orderDtoMono;
    }

    private Mono<OrderDto> buildOrderDto(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .collectList()
                .map(items -> {
                    order.setOrderItemList(items);
                    return orderMapper.toOrderDto(order);
                });
    }
}
