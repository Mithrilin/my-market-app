package ru.yandex.practicum.my_market_app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;
import ru.yandex.practicum.my_market_app.dto.OrderDto;
import ru.yandex.practicum.my_market_app.exception.NotFoundException;
import ru.yandex.practicum.my_market_app.mapper.OrderMapper;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.model.Order;
import ru.yandex.practicum.my_market_app.repository.ItemRepository;
import ru.yandex.practicum.my_market_app.repository.OrderRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        List<Item> testItems = TestDataFactory.createTestItemList(3);
        Map<Item, Integer> itemsWithCount = new HashMap<>();
        for (Item item : testItems) {
            itemsWithCount.put(item, item.getCount());
        }

        testOrder = Order.builder()
                .id(1L)
                .itemsWithCount(itemsWithCount)
                .totalSum(600L)
                .build();

        testOrderDto = OrderDto.builder()
                .id(1L)
                .items(Collections.emptyList())
                .totalSum(600L)
                .build();
    }

    @Test
    @DisplayName("Successful getting Order List")
    void getOrderList_shouldReturnAllOrders() {
        List<Order> orders = Collections.singletonList(testOrder);
        List<OrderDto> orderDtoList = Collections.singletonList(testOrderDto);

        when(orderRepository.findAll()).thenReturn(orders);
        when(orderMapper.toOrderDtoList(orders)).thenReturn(orderDtoList);

        List<OrderDto> result = orderService.getOrderList();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("Successful getting Order by ID")
    void getOrderById_shouldReturnOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);

        OrderDto result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("Getting Order by ID when Order not found")
    void getOrderById_shouldThrowNotFoundExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order with ID = 999 not found");
    }

    @Test
    @DisplayName("Successful Order creation")
    void createOrder_shouldCreateNewOrder() {
        List<Item> itemsInCart = TestDataFactory.createTestItemList(2);
        Order newOrder = Order.builder()
                .id(2L)
                .itemsWithCount(new HashMap<>())
                .totalSum(300L)
                .build();
        OrderDto newOrderDto = OrderDto.builder()
                .id(2L)
                .items(Collections.emptyList())
                .totalSum(300L)
                .build();

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(itemsInCart);
        when(orderMapper.toOrder(anyList(), anyLong())).thenReturn(newOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);
        when(orderMapper.toOrderDto(newOrder)).thenReturn(newOrderDto);
        when(itemRepository.saveAll(anyList())).thenReturn(itemsInCart);

        OrderDto result = orderService.createOrder();

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.totalSum()).isEqualTo(300L);
        verify(itemRepository).findByCountGreaterThanEqual(1);
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Successful order creation with the correct amount")
    void createOrder_shouldCreateOrderWithCorrectTotalSum() {
        List<Item> itemsInCart = List.of(
                TestDataFactory.createTestItem(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItem(2L, "Item 2", "Desc 2", 150L, 3)
        );
        long expectedTotal = 100L * 2 + 150L * 3;

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(itemsInCart);
        when(orderMapper.toOrder(anyList(), eq(expectedTotal))).thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);
        when(itemRepository.saveAll(anyList())).thenReturn(itemsInCart);

        orderService.createOrder();

        verify(orderMapper).toOrder(anyList(), eq(expectedTotal));
    }

    @Test
    @DisplayName("Successful zeroing of the product quantity after order creation")
    void createOrder_shouldResetItemCountsAfterOrderCreation() {
        List<Item> itemsInCart = TestDataFactory.createTestItemList(2);
        itemsInCart.forEach(item -> item.setCount(5));

        when(itemRepository.findByCountGreaterThanEqual(1)).thenReturn(itemsInCart);
        when(orderMapper.toOrder(anyList(), anyLong())).thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toOrderDto(testOrder)).thenReturn(testOrderDto);
        when(itemRepository.saveAll(anyList())).thenReturn(itemsInCart);

        orderService.createOrder();

        verify(itemRepository).saveAll(argThat(items -> {
            for (Item item : items) {
                if (item.getCount() != 0) {
                    return false;
                }
            }
            return true;
        }));
    }
}