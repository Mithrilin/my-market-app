package ru.yandex.practicum.my_market_app.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ItemCartListResponse(

        List<ItemDto> itemDtoList,

        Long total
) {
}
