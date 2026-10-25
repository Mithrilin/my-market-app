package ru.yandex.practicum.market.dto;

public record PaymentStatusDto(

        boolean available,

        String message
) {
}