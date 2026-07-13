package ru.yandex.practicum.my_market_app.dto;

import java.util.List;

public record ItemListResponse(

        List<List<ItemDto>> splittedItemDtoList,

        Paging paging
) {
}
