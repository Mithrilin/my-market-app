package ru.yandex.practicum.payment.exception;

import lombok.Builder;

@Builder
public record PaymentErrorResponse(

        String errorMessage,

        Long balance
) {
}