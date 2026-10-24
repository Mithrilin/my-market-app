package ru.yandex.practicum.market.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import ru.yandex.practicum.market.client.generated.api.PaymentsApi;
import ru.yandex.practicum.market.client.generated.dto.BalanceResponse;
import ru.yandex.practicum.market.client.generated.dto.PaymentRequest;
import ru.yandex.practicum.market.client.generated.dto.PaymentResponse;
import ru.yandex.practicum.market.client.generated.invoker.ApiClient;
import ru.yandex.practicum.market.config.PaymentClientProperties;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.PaymentServiceException;

import java.time.Duration;

@Slf4j
@Component
public class PaymentClientImpl implements PaymentClient {

    private final PaymentsApi paymentsApi;

    public PaymentClientImpl(WebClient.Builder builder, PaymentClientProperties properties) {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new IllegalStateException(
                    "payment.service.base-url is not configured. Set PAYMENT_SERVICE_URL env variable.");
        }
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.timeoutMs())
                .responseTimeout(Duration.ofMillis(properties.timeoutMs()));

        WebClient webClient = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        this.paymentsApi = new PaymentsApi(new ApiClient(webClient).setBasePath(properties.baseUrl()));
    }

    @Override
    public Mono<BalanceResponse> getBalance() {
        return paymentsApi.getBalance()
                .onErrorMap(this::mapPaymentError);
    }

    @Override
    public Mono<PaymentResponse> pay(PaymentRequest request) {
        return paymentsApi.pay(request)
                .onErrorMap(this::mapPaymentError);
    }

    private Throwable mapPaymentError(Throwable e) {
        if (e instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            String body = wcre.getResponseBodyAsString();
            if (status >= 400 && status < 500) {
                return new BadRequestException(
                        "Payment service rejected request: " + status + (body.isBlank() ? "" : " — " + body));
            }
            return new PaymentServiceException(
                    "Payment service is unavailable: " + status + (body.isBlank() ? "" : " — " + body));
        }
        if (e instanceof WebClientRequestException) {
            return new PaymentServiceException("Payment service is unreachable: " + e.getMessage());
        }
        return e;
    }
}