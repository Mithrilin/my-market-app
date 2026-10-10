package ru.yandex.practicum.my_market_app.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.dto.OrderDto;
import ru.yandex.practicum.my_market_app.exception.NotFoundException;
import ru.yandex.practicum.my_market_app.mapper.OrderMapper;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.model.Order;
import ru.yandex.practicum.my_market_app.model.OrderItem;
import ru.yandex.practicum.my_market_app.repository.ItemRepository;
import ru.yandex.practicum.my_market_app.repository.OrderRepository;

import java.util.List;

import static ru.yandex.practicum.my_market_app.service.ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART;

@Slf4j
@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    private final OrderMapper orderMapper;

    @Override
    public List<OrderDto> getOrderList() {
        List<Order> orderList = orderRepository.findAll();
        List<OrderDto> orderDtoList = orderMapper.toOrderDtoList(orderList);
        log.info("Order List with size {} has been returned.", orderDtoList.size());
        return orderDtoList;
    }

    @Override
    public OrderDto getOrderById(Long id) {
        Order order = findOrderById(id);
        OrderDto orderDto = orderMapper.toOrderDto(order);
        log.info("Order with ID = {} returned.", orderDto.id());
        return orderDto;
    }

    @Override
    @Transactional
    public OrderDto createOrder() {
        List<Item> itemList = itemRepository.findByCountGreaterThanEqual(MIN_COUNT_ITEMS_IN_CART);
        long totalSum = 0L;
        for (Item item : itemList) {
            totalSum = totalSum + item.getPrice() * item.getCount();
        }
        Order order = orderMapper.toOrder(itemList, totalSum);
        for (OrderItem orderItem : order.getOrderItemList()) {
            orderItem.setOrder(order);
        }
        Order returnedOrder = orderRepository.save(order);
        OrderDto orderDto = orderMapper.toOrderDto(returnedOrder);
        for (Item item : itemList) {
            item.setCount(0);
        }
        itemRepository.saveAll(itemList);
        log.info("Order with ID = {} created.", orderDto.id());
        return orderDto;
    }

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(String.format("Order with ID = %d not found", orderId)));
    }
}
