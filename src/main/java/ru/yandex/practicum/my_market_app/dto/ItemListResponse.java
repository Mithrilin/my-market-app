package ru.yandex.practicum.my_market_app.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ItemListResponse(

        List<List<ItemDto>> splittedItemDtoList,

        Paging paging
) {
}
