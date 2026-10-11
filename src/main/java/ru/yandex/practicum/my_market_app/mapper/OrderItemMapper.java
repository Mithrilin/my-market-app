package ru.yandex.practicum.my_market_app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.my_market_app.dto.OrderItemDto;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.model.OrderItem;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    OrderItem toOrderItem(Item item);

    OrderItemDto toOrderItemDto(OrderItem orderItem);

    List<OrderItem> toOrderItemList(List<Item> itemList);

    List<OrderItemDto> toOrderItemDtoList(List<OrderItem> orderItemList);
}
