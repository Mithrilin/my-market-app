package ru.yandex.practicum.market.dto;

import java.util.List;

public record CartDto(

        List<ItemDto> itemDtoList,

        Long total
) {
}