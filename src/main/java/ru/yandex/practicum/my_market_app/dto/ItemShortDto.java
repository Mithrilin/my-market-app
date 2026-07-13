package ru.yandex.practicum.my_market_app.dto;

import lombok.Builder;

@Builder
public record ItemShortDto(

        Long id,

        String title,

        Long price,

        Integer count
) {
}
