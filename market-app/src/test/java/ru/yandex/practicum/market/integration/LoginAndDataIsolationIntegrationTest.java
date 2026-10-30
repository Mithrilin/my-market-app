package ru.yandex.practicum.market.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.client.PaymentClient;
import ru.yandex.practicum.market.client.generated.dto.BalanceResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class LoginAndDataIsolationIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @MockitoBean
    private PaymentClient paymentClient;

    private String sessionCookie;
    private Long userOrderId;
    private Long adminOrderId;

    @BeforeEach
    void setUp() {
        when(paymentClient.getBalance(anyLong()))
                .thenReturn(Mono.just(new BalanceResponse().balance(1_000_000L)));

        databaseClient.sql("DELETE FROM cart_items").then().block();
        databaseClient.sql("DELETE FROM order_items").then().block();
        databaseClient.sql("DELETE FROM orders").then().block();

        Long userId = databaseClient.sql("SELECT id FROM users WHERE username='user'")
                .map((r, m) -> r.get("id", Long.class)).one().block();
        Long adminId = databaseClient.sql("SELECT id FROM users WHERE username='admin'")
                .map((r, m) -> r.get("id", Long.class)).one().block();

        userOrderId = databaseClient.sql(
                        "INSERT INTO orders (user_id, total_sum) VALUES ($1, 100) RETURNING id")
                .bind(0, userId).map((r, m) -> r.get("id", Long.class)).one().block();
        adminOrderId = databaseClient.sql(
                        "INSERT INTO orders (user_id, total_sum) VALUES ($1, 200) RETURNING id")
                .bind(0, adminId).map((r, m) -> r.get("id", Long.class)).one().block();

        sessionCookie = loginAndGetSession();
    }

    private String loginAndGetSession() {
        var result = webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("username=user&password=password")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectCookie().exists("SESSION")
                .returnResult(Void.class);
        return result.getResponseCookies().getFirst("SESSION").getValue();
    }

    @Test
    @DisplayName("User sees only own orders")
    void user_seesOnlyOwnOrders() {
        webTestClient.get().uri("/orders")
                .cookie("SESSION", sessionCookie)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("Заказ №" + userOrderId)
                        .doesNotContain("Заказ №" + adminOrderId));
    }

    @Test
    @DisplayName("User cannot access another user's order (403)")
    void user_cannotAccessAnotherUsersOrder() {
        webTestClient.get().uri("/orders/" + adminOrderId)
                .cookie("SESSION", sessionCookie)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Anonymous user is redirected to login from /cart/items")
    void anonymous_isRedirectedToLogin() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*");
    }
}