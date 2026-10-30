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
import ru.yandex.practicum.market.client.generated.dto.PaymentRequest;
import ru.yandex.practicum.market.client.generated.dto.PaymentResponse;
import ru.yandex.practicum.market.client.generated.dto.PaymentStatus;
import ru.yandex.practicum.market.dto.OrderDto;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.ForbiddenException;
import ru.yandex.practicum.market.exception.NotFoundException;
import ru.yandex.practicum.market.exception.PaymentServiceException;
import ru.yandex.practicum.market.mapper.OrderMapper;
import ru.yandex.practicum.market.model.ItemWithCount;
import ru.yandex.practicum.market.model.Order;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

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

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testOrder = TestDataFactory.createTestOrder(1L, USER_ID, 600L);
        testOrderDto = OrderDto.builder()
                .id(1L)
                .items(Collections.emptyList())
                .totalSum(600L)
                .build();
    }

    @Test
    @DisplayName("Successful getting Order List")
    void getOrderList_shouldReturnAllOrders() {
        when(orderRepository.findByUserId(USER_ID)).thenReturn(Flux.just(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);

        StepVerifier.create(orderService.getOrderList(USER_ID))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(1L))
                .verifyComplete();

        verify(orderRepository).findByUserId(USER_ID);
        verify(orderItemRepository).findByOrderId(1L);
    }

    @Test
    @DisplayName("Getting empty Order List")
    void getOrderList_shouldReturnEmptyFluxWhenNoOrders() {
        when(orderRepository.findByUserId(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrderList(USER_ID))
                .verifyComplete();

        verify(orderRepository).findByUserId(USER_ID);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("Successful getting Order by ID")
    void getOrderById_shouldReturnOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);

        StepVerifier.create(orderService.getOrderById(1L, USER_ID))
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

        StepVerifier.create(orderService.getOrderById(999L, USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(NotFoundException.class);
                    assertThat(e.getMessage()).contains("Order with ID = 999 not found");
                })
                .verify();

        verify(orderRepository).findById(999L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("Getting Order by ID of another user — ForbiddenException")
    void getOrderById_shouldThrowForbiddenExceptionWhenOrderBelongsToAnotherUser() {
        when(orderRepository.findById(1L)).thenReturn(Mono.just(testOrder));

        StepVerifier.create(orderService.getOrderById(1L, OTHER_USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ForbiddenException.class);
                    assertThat(e.getMessage()).contains("Access to order with ID = 1 is forbidden");
                })
                .verify();

        verify(orderRepository).findById(1L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("Successful Order creation with successful payment")
    void createOrder_shouldCreateNewOrder() {
        List<ItemWithCount> itemsInCart = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItemWithCount(2L, "Item 2", "Desc 2", 200L, 3));

        Order savedOrder = TestDataFactory.createTestOrder(2L, USER_ID, 800L);
        OrderDto newOrderDto = OrderDto.builder()
                .id(2L)
                .items(Collections.emptyList())
                .totalSum(800L)
                .build();

        when(itemRepository.findCartItemsByUserId(USER_ID, 1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse()
                        .userId(USER_ID).amount(800L).newBalance(9200L).status(PaymentStatus.SUCCESS)));
        when(orderPersistenceService.saveNewOrder(anyList(), eq(800L), eq(USER_ID)))
                .thenReturn(Mono.just(savedOrder));
        when(orderMapper.toOrderDto(savedOrder)).thenReturn(newOrderDto);

        StepVerifier.create(orderService.createOrder(USER_ID))
                .assertNext(dto -> {
                    assertThat(dto).isNotNull();
                    assertThat(dto.id()).isEqualTo(2L);
                    assertThat(dto.totalSum()).isEqualTo(800L);
                })
                .verifyComplete();

        verify(paymentClient).pay(argThat(req ->
                req.getUserId().equals(USER_ID) && req.getAmount().equals(800L)));
        verify(orderPersistenceService).saveNewOrder(anyList(), eq(800L), eq(USER_ID));
    }

    @Test
    @DisplayName("Order creation fails when payment is declined (4xx)")
    void createOrder_shouldFailWhenPaymentDeclined() {
        List<ItemWithCount> itemsInCart = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2));

        when(itemRepository.findCartItemsByUserId(USER_ID, 1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new BadRequestException("Insufficient funds")));

        StepVerifier.create(orderService.createOrder(USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Оплата не прошла");
                })
                .verify();

        verify(paymentClient).pay(any(PaymentRequest.class));
        verify(orderPersistenceService, never()).saveNewOrder(anyList(), any(Long.class), any(Long.class));
    }

    @Test
    @DisplayName("Order creation fails with PaymentServiceException when payment-service is down")
    void createOrder_shouldFailWithServiceUnavailableWhenPaymentServiceDown() {
        List<ItemWithCount> itemsInCart = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2));

        when(itemRepository.findCartItemsByUserId(USER_ID, 1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new PaymentServiceException("Payment service is unavailable")));

        StepVerifier.create(orderService.createOrder(USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(PaymentServiceException.class);
                    assertThat(e.getMessage()).contains("Payment service is unavailable");
                })
                .verify();

        verify(paymentClient).pay(any(PaymentRequest.class));
        verify(orderPersistenceService, never()).saveNewOrder(anyList(), any(Long.class), any(Long.class));
    }

    @Test
    @DisplayName("Successful order creation with the correct total amount")
    void createOrder_shouldCreateOrderWithCorrectTotalSum() {
        List<ItemWithCount> itemsInCart = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItemWithCount(2L, "Item 2", "Desc 2", 150L, 3));
        long expectedTotal = 100L * 2 + 150L * 3;

        Order savedOrder = TestDataFactory.createTestOrder(2L, USER_ID, expectedTotal);
        OrderDto newOrderDto = OrderDto.builder()
                .id(2L)
                .items(Collections.emptyList())
                .totalSum(expectedTotal)
                .build();

        when(itemRepository.findCartItemsByUserId(USER_ID, 1)).thenReturn(Flux.fromIterable(itemsInCart));
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse().amount(expectedTotal).newBalance(0L)
                        .status(PaymentStatus.SUCCESS)));
        when(orderPersistenceService.saveNewOrder(anyList(), eq(expectedTotal), eq(USER_ID)))
                .thenReturn(Mono.just(savedOrder));
        when(orderMapper.toOrderDto(savedOrder)).thenReturn(newOrderDto);

        StepVerifier.create(orderService.createOrder(USER_ID))
                .assertNext(dto -> assertThat(dto.totalSum()).isEqualTo(expectedTotal))
                .verifyComplete();

        verify(paymentClient).pay(argThat(req ->
                req.getUserId().equals(USER_ID) && req.getAmount().equals(expectedTotal)));
        verify(orderPersistenceService).saveNewOrder(anyList(), eq(expectedTotal), eq(USER_ID));
    }

    @Test
    @DisplayName("Creating Order with empty cart fails with BadRequestException, no payment, no DB write")
    void createOrder_shouldFailWhenCartIsEmpty() {
        when(itemRepository.findCartItemsByUserId(USER_ID, 1)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.createOrder(USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Корзина пуста");
                })
                .verify();

        verify(paymentClient, never()).pay(any(PaymentRequest.class));
        verify(orderPersistenceService, never()).saveNewOrder(anyList(), any(Long.class), any(Long.class));
    }
}