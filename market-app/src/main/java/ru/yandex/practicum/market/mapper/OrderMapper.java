package ru.yandex.practicum.market.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.market.dto.OrderDto;
import ru.yandex.practicum.market.model.Order;

import java.util.List;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "items", source = "orderItemList")
    OrderDto toOrderDto(Order order);

    List<OrderDto> toOrderDtoList(List<Order> orderList);
}
