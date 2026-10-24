package ru.yandex.practicum.market.dto;

public record CartPageResponse(

        CartDto cart,

        PaymentStatusDto payment
) {
}
