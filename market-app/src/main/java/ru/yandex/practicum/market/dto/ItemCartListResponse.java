package ru.yandex.practicum.market.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ItemCartListResponse(

        List<ItemDto> itemDtoList,

        Long total
) {
}
