package ru.yandex.practicum.market.dto;

import lombok.Builder;

@Builder
public record ItemDto(

        Long id,

        String title,

        String description,

        String imgPath,

        Long price,

        Integer count
) {
}
