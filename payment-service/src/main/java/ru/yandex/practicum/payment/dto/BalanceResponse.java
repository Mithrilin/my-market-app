package ru.yandex.practicum.payment.dto;

import lombok.Builder;

@Builder
public record BalanceResponse(

        Long balance
) {
}