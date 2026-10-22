package ru.yandex.practicum.market.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CartPageResponse(

        List<ItemDto> itemDtoList,

        Long total,

        boolean paymentAvailable,

        String paymentMessage
) {
}
