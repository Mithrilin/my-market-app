package ru.yandex.practicum.my_market_app.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderDto(

        Long id,

        List<ItemShortDto> items,

        Long totalSum
) {
}
