package ru.yandex.practicum.payment.integration;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class PaymentSecurityIntegrationTest {

    private static final Long USER_ID = 1L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("payment-bd")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Container
    static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer("quay.io/keycloak/keycloak:26.1.2")
                    .withRealmImportFile("keycloak/realm-export.json");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                        + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> KEYCLOAK.getAuthServerUrl() + "/realms/market");
    }

    private String obtainClientCredentialsToken(String clientId, String secret) {
        String tokenUri = KEYCLOAK.getAuthServerUrl() + "/realms/market/protocol/openid-connect/token";
        Map<String, Object> response = WebClient.create()
                .post().uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + secret)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        return (String) response.get("access_token");
    }

    private void insertAccount(long balance) {
        databaseClient.sql("DELETE FROM accounts").then().block();
        databaseClient.sql("INSERT INTO accounts (user_id, balance) VALUES ($1, $2)")
                .bind(0, USER_ID).bind(1, balance).then().block();
    }

    @Test
    @DisplayName("Token with SERVICE role can call balance and pay")
    void serviceToken_canAccessPaymentEndpoints() {
        insertAccount(1000L);
        String token = obtainClientCredentialsToken("market-app", "market-secret");

        webTestClient.get().uri("/api/payments/balance?userId=1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1000);

        webTestClient.post().uri("/api/payments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\": 1, \"amount\": 300}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS");

        Long balance = databaseClient.sql("SELECT balance FROM accounts WHERE user_id = $1")
                .bind(0, USER_ID).map((r, m) -> r.get("balance", Long.class)).one().block();
        assertThat(balance).isEqualTo(700L);
    }

    @Test
    @DisplayName("Request without token returns 401")
    void noToken_returnsUnauthorized() {
        webTestClient.get().uri("/api/payments/balance?userId=1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Token without SERVICE role returns 403")
    void tokenWithoutServiceRole_returnsForbidden() {
        String token = obtainClientCredentialsToken("no-service-client", "no-service-secret");

        webTestClient.get().uri("/api/payments/balance?userId=1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}