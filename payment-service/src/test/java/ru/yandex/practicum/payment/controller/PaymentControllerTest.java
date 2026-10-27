package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.SecurityConfig;
import ru.yandex.practicum.payment.dto.generated.BalanceResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentStatus;
import ru.yandex.practicum.payment.exception.ErrorHandler;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.service.PaymentService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import({SecurityConfig.class, ErrorHandler.class})
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    private SecurityMockServerConfigurers.JwtMutator mockJwtWithServiceRole() {
        return SecurityMockServerConfigurers.mockJwt()
                .jwt(jwt -> jwt.claim("resource_access",
                        Map.of("market-app", Map.of("roles", List.of("SERVICE")))))
                .authorities(new SimpleGrantedAuthority("SERVICE"));
    }

    private SecurityMockServerConfigurers.JwtMutator mockJwtWithoutServiceRole() {
        return SecurityMockServerConfigurers.mockJwt()
                .jwt(jwt -> jwt.claim("resource_access", Map.of()));
    }

    @Test
    @DisplayName("GET /api/payments/balance returns balance")
    void getBalance_shouldReturnBalance() {
        when(paymentService.getBalance(anyLong()))
                .thenReturn(Mono.just(new BalanceResponse().balance(1000L)));

        webTestClient.mutateWith(mockJwtWithServiceRole())
                .get().uri("/api/payments/balance?userId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1000);
    }

    @Test
    @DisplayName("POST /api/payments returns success")
    void pay_shouldReturnSuccessResponse() {
        when(paymentService.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse()
                        .userId(1L).amount(300L).newBalance(700L).status(PaymentStatus.SUCCESS)));

        webTestClient.mutateWith(mockJwtWithServiceRole())
                .post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId": 1, "amount": 300}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.amount").isEqualTo(300)
                .jsonPath("$.newBalance").isEqualTo(700)
                .jsonPath("$.status").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST /api/payments returns 400 on insufficient funds")
    void pay_shouldReturnBadRequestOnInsufficientFunds() {
        when(paymentService.pay(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new InsufficientFundsException("Insufficient funds")));

        webTestClient.mutateWith(mockJwtWithServiceRole())
                .post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId": 1, "amount": 99999}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorMessage").isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("POST /api/payments returns 400 on validation error (negative amount)")
    void pay_shouldReturnBadRequestOnValidationError() {
        webTestClient.mutateWith(mockJwtWithServiceRole())
                .post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId": 1, "amount": -100}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/payments returns 400 on missing amount")
    void pay_shouldReturnBadRequestOnMissingAmount() {
        webTestClient.mutateWith(mockJwtWithServiceRole())
                .post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId": 1}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Request without JWT returns 401")
    void requestWithoutJwt_shouldReturnUnauthorized() {
        webTestClient.get().uri("/api/payments/balance?userId=1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Request without SERVICE role returns 403")
    void requestWithoutServiceRole_shouldReturnForbidden() {
        webTestClient.mutateWith(mockJwtWithoutServiceRole())
                .get().uri("/api/payments/balance?userId=1")
                .exchange()
                .expectStatus().isForbidden();
    }
}