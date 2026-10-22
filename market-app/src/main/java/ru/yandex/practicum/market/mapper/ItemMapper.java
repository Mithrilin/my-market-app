package ru.yandex.practicum.market.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.model.Item;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toItemDto(Item item);

    List<ItemDto> toItemDtoList(List<Item> itemList);
}
