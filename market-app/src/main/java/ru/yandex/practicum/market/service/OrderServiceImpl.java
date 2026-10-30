package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.client.PaymentClient;
import ru.yandex.practicum.market.client.generated.dto.PaymentRequest;
import ru.yandex.practicum.market.dto.OrderDto;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.ForbiddenException;
import ru.yandex.practicum.market.exception.NotFoundException;
import ru.yandex.practicum.market.mapper.OrderMapper;
import ru.yandex.practicum.market.model.Order;
import ru.yandex.practicum.market.repository.ItemRepository;
import ru.yandex.practicum.market.repository.OrderItemRepository;
import ru.yandex.practicum.market.repository.OrderRepository;

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
    private final OrderPersistenceService orderPersistenceService;

    @Override
    public Flux<OrderDto> getOrderList(Long userId) {
        Flux<OrderDto> orderDtoFlux = orderRepository.findByUserId(userId)
                .concatMap(this::buildOrderDto);
        log.info("Order List has been returned for user {}.", userId);
        return orderDtoFlux;
    }

    @Override
    public Mono<OrderDto> getOrderById(Long id, Long userId) {
        Mono<OrderDto> orderDtoMono = orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        String.format("Order with ID = %d not found", id))))
                .flatMap(order -> {
                    if (!order.getUserId().equals(userId)) {
                        return Mono.error(new ForbiddenException(
                                String.format("Access to order with ID = %d is forbidden", id)));
                    }
                    return buildOrderDto(order);
                });
        log.info("Order with ID = {} returned for user {}.", id, userId);
        return orderDtoMono;
    }

    @Override
    public Mono<OrderDto> createOrder(Long userId) {
        Mono<OrderDto> orderDtoMono = itemRepository.findCartItemsByUserId(userId, MIN_COUNT_ITEMS_IN_CART)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new BadRequestException(
                                "Корзина пуста, невозможно оформить заказ"));
                    }

                    long totalSum = items.stream()
                            .mapToLong(i -> i.getPrice() * i.getCount())
                            .sum();

                    Mono<Void> paymentStep = paymentClient
                            .pay(new PaymentRequest().userId(userId).amount(totalSum))
                            .onErrorMap(BadRequestException.class,
                                    e -> new BadRequestException("Оплата не прошла: " + e.getMessage()))
                            .then();

                    return paymentStep.then(Mono.defer(
                            () -> orderPersistenceService.saveNewOrder(items, totalSum, userId)));
                })
                .map(orderMapper::toOrderDto);
        log.info("Order created for user {}.", userId);
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