package ru.yandex.practicum.payment.dto;

import lombok.Builder;
import ru.yandex.practicum.payment.enums.PaymentStatus;

@Builder
public record PaymentResponse(

        Long orderId,

        Long amount,

        Long newBalance,

        PaymentStatus status
) {
}