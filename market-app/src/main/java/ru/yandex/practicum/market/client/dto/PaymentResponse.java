package ru.yandex.practicum.market.client.dto;

public record PaymentResponse(

        Long orderId,

        Long amount,

        Long newBalance,

        String status
) {
}