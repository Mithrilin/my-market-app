package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentRequest;
import ru.yandex.practicum.payment.dto.PaymentResponse;
import ru.yandex.practicum.payment.enums.PaymentStatus;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("GET /api/payments/balance returns balance")
    void getBalance_shouldReturnBalance() {
        when(paymentService.getBalance()).thenReturn(Mono.just(new BalanceResponse(1000L)));

        webTestClient.get().uri("/api/payments/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1000);
    }

    @Test
    @DisplayName("POST /api/payments returns success")
    void pay_shouldReturnSuccessResponse() {
        when(paymentService.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse(300L, 700L, PaymentStatus.SUCCESS)));

        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 300}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.amount").isEqualTo(300)
                .jsonPath("$.newBalance").isEqualTo(700)
                .jsonPath("$.status").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST /api/payments returns 400 on insufficient funds")
    void pay_shouldReturnBadRequestOnInsufficientFunds() {
        when(paymentService.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new InsufficientFundsException("Insufficient funds")));

        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 99999}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorMessage").isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("POST /api/payments returns 400 on validation error (negative amount)")
    void pay_shouldReturnBadRequestOnValidationError() {
        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": -100}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/payments returns 400 on missing amount")
    void pay_shouldReturnBadRequestOnMissingAmount() {
        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
