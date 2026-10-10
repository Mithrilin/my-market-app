package ru.yandex.practicum.my_market_app.dto;

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
