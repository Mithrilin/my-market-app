package ru.yandex.practicum.market.client.dto;

public record PaymentResponse(

        Long amount,

        Long newBalance,

        String status
) {
}