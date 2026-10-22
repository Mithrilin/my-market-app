package ru.yandex.practicum.market.client.dto;

public record PaymentRequest(

        Long orderId,

        Long amount
) {
}
