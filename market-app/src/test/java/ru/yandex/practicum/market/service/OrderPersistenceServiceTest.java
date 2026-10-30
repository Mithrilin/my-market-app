package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.model.ItemWithCount;
import ru.yandex.practicum.market.model.Order;
import ru.yandex.practicum.market.model.OrderItem;
import ru.yandex.practicum.market.repository.CartItemRepository;
import ru.yandex.practicum.market.repository.OrderItemRepository;
import ru.yandex.practicum.market.repository.OrderRepository;
import ru.yandex.practicum.market.util.TestDataFactory;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderPersistenceService orderPersistenceService;

    @Test
    @DisplayName("Successful save: Order, OrderItems saved, cart cleared")
    void saveNewOrder_shouldSaveOrderAndItemsAndClearCart() {
        List<ItemWithCount> items = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItemWithCount(2L, "Item 2", "Desc 2", 200L, 3));

        Order savedOrder = TestDataFactory.createTestOrder(10L, USER_ID, 800L);
        List<OrderItem> savedItems = List.of(
                TestDataFactory.createTestOrderItem(100L, 10L, "Item 1", 100L, 2),
                TestDataFactory.createTestOrderItem(101L, 10L, "Item 2", 200L, 3));

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(any(Iterable.class))).thenReturn(Flux.fromIterable(savedItems));
        when(cartItemRepository.deleteAllByUserId(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(orderPersistenceService.saveNewOrder(items, 800L, USER_ID))
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(10L);
                    assertThat(order.getUserId()).isEqualTo(USER_ID);
                    assertThat(order.getTotalSum()).isEqualTo(800L);
                    assertThat(order.getOrderItemList()).hasSize(2);
                })
                .verifyComplete();

        verify(orderRepository).save(argThat(o -> o.getTotalSum() != null
                && o.getTotalSum().equals(800L)
                && o.getUserId().equals(USER_ID)));

        verify(orderItemRepository).saveAll(argThat((Iterable<OrderItem> list) -> {
            List<OrderItem> orderItems = StreamSupport.stream(list.spliterator(), false).toList();
            return orderItems.size() == 2
                    && orderItems.stream().allMatch(oi -> oi.getOrderId().equals(10L));
        }));

        verify(cartItemRepository).deleteAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("Empty cart: Order saved, no OrderItems, cart cleared")
    void saveNewOrder_withEmptyCart_shouldSaveOnlyOrder() {
        Order savedOrder = TestDataFactory.createTestOrder(20L, USER_ID, 0L);

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartItemRepository.deleteAllByUserId(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(orderPersistenceService.saveNewOrder(List.of(), 0L, USER_ID))
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(20L);
                    assertThat(order.getTotalSum()).isZero();
                    assertThat(order.getOrderItemList()).isEmpty();
                })
                .verifyComplete();

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any(Iterable.class));
        verify(cartItemRepository).deleteAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("Error saving Order propagates")
    void saveNewOrder_whenOrderSaveFails_shouldPropagateError() {
        List<ItemWithCount> items = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(orderPersistenceService.saveNewOrder(items, 200L, USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(RuntimeException.class);
                    assertThat(e.getMessage()).contains("DB error");
                })
                .verify();

        verify(orderItemRepository, never()).saveAll(any(Iterable.class));
        verify(cartItemRepository, never()).deleteAllByUserId(any(Long.class));
    }
}