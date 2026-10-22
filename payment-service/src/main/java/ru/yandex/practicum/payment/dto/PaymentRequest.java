package ru.yandex.practicum.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PaymentRequest(

        @NotNull(message = "Order id cannot be null.")
        @Positive(message = "Order id must be greater than 0.")
        Long orderId,

        @NotNull(message = "Amount cannot be null.")
        @Positive(message = "Amount must be greater than 0.")
        Long amount
) {
}