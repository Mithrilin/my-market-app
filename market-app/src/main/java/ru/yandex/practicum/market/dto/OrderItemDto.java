package ru.yandex.practicum.market.dto;

import lombok.Builder;

@Builder
public record OrderItemDto(

        Long id,

        Long orderId,

        String title,

        Long price,

        Integer count
) {
}
