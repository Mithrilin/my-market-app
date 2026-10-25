package ru.yandex.practicum.payment.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.generated.PaymentsApi;
import ru.yandex.practicum.payment.dto.generated.BalanceResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;

@Slf4j
@RestController
@AllArgsConstructor
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        log.info("Getting current balance.");
        return paymentService.getBalance()
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> pay(Mono<PaymentRequest> paymentRequest,
                                                     ServerWebExchange exchange) {
        return paymentRequest.flatMap(request -> {
            log.info("Processing payment with amount {}.", request.getAmount());
            return paymentService.pay(request)
                    .map(ResponseEntity::ok);
        });
    }
}