package ru.yandex.practicum.my_market_app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.my_market_app.dto.OrderDto;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.model.Order;

import java.util.List;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "orderItemList", source = "itemList")
    Order toOrder(List<Item> itemList, Long totalSum);

    @Mapping(target = "items", source = "orderItemList")
    OrderDto toOrderDto(Order order);

    List<OrderDto> toOrderDtoList(List<Order> orderList);
}
