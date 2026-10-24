package ru.yandex.practicum.market.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import ru.yandex.practicum.market.client.dto.BalanceResponse;
import ru.yandex.practicum.market.client.dto.PaymentRequest;
import ru.yandex.practicum.market.client.dto.PaymentResponse;
import ru.yandex.practicum.market.config.PaymentClientProperties;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.PaymentServiceException;

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
                .onStatus(HttpStatusCode::is4xxClientError, this::mapClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::mapServerError)
                .bodyToMono(BalanceResponse.class)
                .onErrorMap(WebClientRequestException.class,
                        e -> new PaymentServiceException(
                                "Payment service is unreachable: " + e.getMessage()));
    }

    @Override
    public Mono<PaymentResponse> pay(PaymentRequest request) {
        return webClient.post()
                .uri("/api/payments")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::mapServerError)
                .bodyToMono(PaymentResponse.class)
                .onErrorMap(WebClientRequestException.class,
                        e -> new PaymentServiceException(
                                "Payment service is unreachable: " + e.getMessage()));
    }

    private Mono<? extends Throwable> mapClientError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new BadRequestException(
                        "Payment service rejected request: " + response.statusCode() +
                                (body.isBlank() ? "" : " — " + body))));
    }

    private Mono<? extends Throwable> mapServerError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new PaymentServiceException(
                        "Payment service is unavailable: " + response.statusCode() +
                                (body.isBlank() ? "" : " — " + body))));
    }
}