package ru.yandex.practicum.market.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ItemListResponse(

        List<List<ItemDto>> splittedItemDtoList,

        Paging paging
) {
}
