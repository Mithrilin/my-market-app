package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.client.PaymentClient;
import ru.yandex.practicum.market.client.dto.PaymentRequest;
import ru.yandex.practicum.market.client.dto.PaymentResponse;
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
import ru.yandex.practicum.market.util.TestDataFactory;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testOrder = TestDataFactory.createTestOrder(1L, 600L);
        testOrderDto = OrderDto.builder()
                .id(1L)
                .items(Collections.emptyList())
                .totalSum(600L)
                .build();
    }

    @Test
    @DisplayName("Successful getting Order List")
    void getOrderList_shouldReturnAllOrders() {
        when(orderRepository.findAll()).thenReturn(Flux.just(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);

        StepVerifier.create(orderService.getOrderList())
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(1L))
                .verifyComplete();

        verify(orderRepository).findAll();
        verify(orderItemRepository).findByOrderId(1L);
    }

    @Test
    @DisplayName("Getting empty Order List")
    void getOrderList_shouldReturnEmptyFluxWhenNoOrders() {
        when(orderRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrderList())
                .verifyComplete();

        verify(orderRepository).findAll();
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("Successful getting Order by ID")
    void getOrderById_shouldReturnOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);

        StepVerifier.create(orderService.getOrderById(1L))
                .assertNext(dto -> {
                    assertThat(dto).isNotNull();
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.totalSum()).isEqualTo(600L);
                })
                .verifyComplete();

        verify(orderRepository).findById(1L);
        verify(orderItemRepository).findByOrderId(1L);
    }

    @Test
    @DisplayName("Getting Order by ID when Order not found")
    void getOrderById_shouldThrowNotFoundExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById(999L))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(NotFoundException.class);
                    assertThat(e.getMessage()).contains("Order with ID = 999 not found");
                })
                .verify();

        verify(orderRepository).findById(999L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("Successful Order creation with successful payment")
    void createOrder_shouldCreateNewOrder() {
        List<Item> itemsInCart = List.of(
                TestDataFactory.createTestItem(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItem(2L, "Item 2", "Desc 2", 200L, 3));

        Order newOrder = TestDataFactory.createTestOrder(2L, 800L);
        OrderDto newOrderDto = OrderDto.builder()
                .id(2L)
                .items(Collections.emptyList())
                .totalSum(800L)
                .build();

        List<OrderItem> savedOrderItems = List.of(
                TestDataFactory.createTestOrderItem(1L, 2L, "Item 1", 100L, 2),
                TestDataFactory.createTestOrderItem(2L, 2L, "Item 2", 200L, 3));

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(newOrder));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse(2L, 800L, 9200L, "SUCCESS")));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(savedOrderItems));
        when(itemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(itemsInCart));
        when(orderMapper.toOrderDto(newOrder)).thenReturn(newOrderDto);

        StepVerifier.create(orderService.createOrder())
                .assertNext(dto -> {
                    assertThat(dto).isNotNull();
                    assertThat(dto.id()).isEqualTo(2L);
                    assertThat(dto.totalSum()).isEqualTo(800L);
                })
                .verifyComplete();

        verify(paymentClient).pay(argThat(req -> req.orderId() != null && req.orderId().equals(2L)));
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(itemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Order creation fails when payment is declined")
    void createOrder_shouldFailWhenPaymentDeclined() {
        List<Item> itemsInCart = List.of(
                TestDataFactory.createTestItem(1L, "Item 1", "Desc 1", 100L, 2));

        Order savedOrder = TestDataFactory.createTestOrder(10L, 200L);

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new BadRequestException("Insufficient funds")));

        StepVerifier.create(orderService.createOrder())
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Оплата не прошла");
                })
                .verify();

        verify(orderRepository).save(any(Order.class));
        verify(paymentClient).pay(argThat(req -> req.orderId() != null && req.orderId().equals(10L)));
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(itemRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Successful order creation with the correct total amount")
    void createOrder_shouldCreateOrderWithCorrectTotalSum() {
        List<Item> itemsInCart = List.of(
                TestDataFactory.createTestItem(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItem(2L, "Item 2", "Desc 2", 150L, 3));
        long expectedTotal = 100L * 2 + 150L * 3;

        Order newOrder = TestDataFactory.createTestOrder(2L, expectedTotal);
        OrderDto newOrderDto = OrderDto.builder()
                .id(2L)
                .items(Collections.emptyList())
                .totalSum(expectedTotal)
                .build();

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(newOrder));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse(2L, expectedTotal, 0L, "SUCCESS")));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.empty());
        when(itemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(itemsInCart));
        when(orderMapper.toOrderDto(newOrder)).thenReturn(newOrderDto);

        StepVerifier.create(orderService.createOrder())
                .assertNext(dto -> assertThat(dto.totalSum()).isEqualTo(expectedTotal))
                .verifyComplete();

        verify(orderRepository).save(argThat(o -> o.getTotalSum() != null
                && o.getTotalSum().equals(expectedTotal)));
        verify(paymentClient).pay(argThat(req -> req.orderId() != null
                && req.orderId().equals(2L)
                && req.amount() != null
                && req.amount().equals(expectedTotal)));
    }

    @Test
    @DisplayName("Creating Order when cart is empty — payment is skipped")
    void createOrder_shouldWorkWhenCartIsEmpty() {
        Order emptyOrder = TestDataFactory.createTestOrder(1L, 0L);
        OrderDto emptyOrderDto = OrderDto.builder()
                .id(1L)
                .items(Collections.emptyList())
                .totalSum(0L)
                .build();

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(Flux.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(emptyOrder));
        when(orderMapper.toOrderDto(emptyOrder)).thenReturn(emptyOrderDto);

        StepVerifier.create(orderService.createOrder())
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.totalSum()).isEqualTo(0L);
                })
                .verifyComplete();

        verify(paymentClient, never()).pay(any(PaymentRequest.class));
        verify(orderRepository).save(argThat(o -> o.getTotalSum() != null && o.getTotalSum() == 0L));
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(itemRepository, never()).saveAll(anyList());
    }
}