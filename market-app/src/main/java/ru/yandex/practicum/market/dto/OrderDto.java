package ru.yandex.practicum.market.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderDto(

        Long id,

        List<OrderItemDto> items,

        Long totalSum
) {
}
