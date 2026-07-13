package ru.yandex.practicum.my_market_app.dto;

import java.util.List;

public record ItemCartListResponse(

        List<ItemDto> itemDtoList,

        Long total
) {
}
