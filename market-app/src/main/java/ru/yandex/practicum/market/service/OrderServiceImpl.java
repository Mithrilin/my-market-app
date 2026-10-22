package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.client.PaymentClient;
import ru.yandex.practicum.market.client.dto.PaymentRequest;
import ru.yandex.practicum.market.dto.OrderDto;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.NotFoundException;
import ru.yandex.practicum.market.mapper.OrderMapper;
import ru.yandex.practicum.market.model.Item;
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

    private final PaymentClient paymentClient;

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
    @CacheEvict(cacheNames = {"item", "itemList", "cartItems"}, allEntries = true)
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
                                Mono<Void> paymentStep = items.isEmpty()
                                        ? Mono.empty()
                                        : paymentClient.pay(new PaymentRequest(savedOrder.getId(), totalSum))
                                        .onErrorMap(e -> new BadRequestException(
                                                "Оплата не прошла: " + e.getMessage()))
                                        .then();

                                return paymentStep.then(Mono.defer(
                                        () -> saveOrderItemsAndClearCart(savedOrder, items)));
                            });
                })
                .map(orderMapper::toOrderDto);
        log.info("Order created.");
        return orderDtoMono;
    }

    private Mono<Void> payForOrder(long totalSum) {
        return paymentClient.pay(new PaymentRequest(null, totalSum))
                .onErrorMap(e -> new BadRequestException(
                        "Оплата не прошла: " + e.getMessage()))
                .then();
    }

    private Mono<Order> saveOrderItemsAndClearCart(Order savedOrder, List<Item> items) {
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
