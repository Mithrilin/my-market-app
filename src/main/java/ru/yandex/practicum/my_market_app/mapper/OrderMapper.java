package ru.yandex.practicum.my_market_app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.yandex.practicum.my_market_app.dto.OrderDto;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.model.Order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "itemsWithCount", qualifiedByName = "getItemsWithCount", source = "itemList")
    Order toOrder(List<Item> itemList, Long totalSum);

    @Mapping(target = "items", qualifiedByName = "getItemList", source = "itemsWithCount")
    OrderDto toOrderDto(Order order);

    List<OrderDto> toOrderDtoList(List<Order> orderList);

    @Named("getItemList")
    default List<Item> getItemList(Map<Item, Integer> itemsWithCount) {
        List<Item> itemList = List.copyOf(itemsWithCount.keySet());
        for (Item item : itemList) {
            item.setCount(itemsWithCount.get(item));
        }
        return itemList;
    }

    @Named("getItemsWithCount")
    default Map<Item, Integer> getItemsWithCount(List<Item> itemList) {
        Map<Item, Integer> itemsWithCount = new HashMap<>();
        for (Item item : itemList) {
            itemsWithCount.put(item, item.getCount());
        }
        return itemsWithCount;
    }
}
