package ru.yandex.practicum.my_market_app.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemShortDto;
import ru.yandex.practicum.my_market_app.model.Item;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toItemDto(Item item);

    ItemShortDto toItemShortDto(Item item);

    List<ItemDto> toItemDtoList(List<Item> itemList);

    List<ItemShortDto> toItemShortDtoList(List<Item> itemList);
}
