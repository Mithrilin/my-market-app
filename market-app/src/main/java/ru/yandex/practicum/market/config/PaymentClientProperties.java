package ru.yandex.practicum.market.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.service")
public record PaymentClientProperties(

        String baseUrl,

        long timeoutMs
) {
}