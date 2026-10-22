package ru.yandex.practicum.market.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import ru.yandex.practicum.market.client.dto.BalanceResponse;
import ru.yandex.practicum.market.client.dto.PaymentRequest;
import ru.yandex.practicum.market.client.dto.PaymentResponse;
import ru.yandex.practicum.market.config.PaymentClientProperties;
import ru.yandex.practicum.market.exception.BadRequestException;

import java.time.Duration;

@Slf4j
@Component
public class PaymentClientImpl implements PaymentClient {

    private final WebClient webClient;

    public PaymentClientImpl(WebClient.Builder builder, PaymentClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.timeoutMs())
                .responseTimeout(Duration.ofMillis(properties.timeoutMs()));

        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<BalanceResponse> getBalance() {
        return webClient.get()
                .uri("/api/payments/balance")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new BadRequestException("Payment service returned error")))
                .bodyToMono(BalanceResponse.class)
                .doOnError(e -> log.error("Failed to get balance from payment service", e));
    }

    @Override
    public Mono<PaymentResponse> pay(PaymentRequest request) {
        return webClient.post()
                .uri("/api/payments")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new BadRequestException(body))))
                .bodyToMono(PaymentResponse.class)
                .doOnError(e -> log.error("Payment failed for order {}", request.orderId(), e));
    }
}