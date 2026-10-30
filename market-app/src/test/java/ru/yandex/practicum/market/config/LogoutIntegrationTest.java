package ru.yandex.practicum.market.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.market.client.PaymentClient;
import ru.yandex.practicum.market.service.OrderService;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class LogoutIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentClient paymentClient;

    @Test
    @DisplayName("Login -> private page -> logout -> old cookie gives redirect to login")
    void logout_shouldInvalidateSessionAndClearCookie() {
        when(orderService.getOrderList(anyLong())).thenReturn(Flux.empty());

        var loginResult = webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("username=user&password=password")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectCookie().exists("SESSION")
                .returnResult(Void.class);

        String sessionCookie = loginResult.getResponseCookies().getFirst("SESSION").getValue();

        webTestClient.get().uri("/orders")
                .cookie("SESSION", sessionCookie)
                .exchange()
                .expectStatus().isOk();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/logout")
                .cookie("SESSION", sessionCookie)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*")
                .expectCookie().maxAge("SESSION", Duration.ZERO);

        webTestClient.get().uri("/orders")
                .cookie("SESSION", sessionCookie)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*");
    }
}