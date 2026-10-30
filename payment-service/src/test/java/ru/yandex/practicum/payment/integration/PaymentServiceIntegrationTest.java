package ru.yandex.practicum.payment.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentStatus;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.repository.AccountRepository;
import ru.yandex.practicum.payment.service.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @BeforeEach
    void setUp() {
        databaseClient.sql("DELETE FROM accounts").then().block();
    }

    private void insertAccount(long balance) {
        databaseClient.sql("INSERT INTO accounts (user_id, balance) VALUES ($1, $2)")
                .bind(0, USER_ID)
                .bind(1, balance)
                .then()
                .block();
    }

    private long currentBalance() {
        return accountRepository.findById(USER_ID).block().getBalance();
    }

    @Test
    @DisplayName("Successful payment decreases balance in real DB")
    void pay_shouldDecreaseBalanceInDb() {
        insertAccount(1000L);

        PaymentRequest request = new PaymentRequest().userId(USER_ID).amount(300L);

        StepVerifier.create(paymentService.pay(request))
                .assertNext(r -> {
                    assertThat(r.getUserId()).isEqualTo(USER_ID);
                    assertThat(r.getAmount()).isEqualTo(300L);
                    assertThat(r.getNewBalance()).isEqualTo(700L);
                    assertThat(r.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                })
                .verifyComplete();

        assertThat(currentBalance()).isEqualTo(700L);
    }

    @Test
    @DisplayName("Payment fails on insufficient funds and balance stays untouched")
    void pay_shouldFailAndNotChangeBalanceWhenInsufficientFunds() {
        insertAccount(100L);

        PaymentRequest request = new PaymentRequest().userId(USER_ID).amount(500L);

        StepVerifier.create(paymentService.pay(request))
                .expectErrorSatisfies(e -> assertThat(e).isInstanceOf(InsufficientFundsException.class))
                .verify();

        assertThat(currentBalance()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Two concurrent payments both succeed and final balance is consistent")
    void pay_shouldHandleConcurrentPaymentsWithRowLock() {
        insertAccount(1000L);

        PaymentRequest first = new PaymentRequest().userId(USER_ID).amount(300L);
        PaymentRequest second = new PaymentRequest().userId(USER_ID).amount(300L);

        StepVerifier.create(Mono.zip(paymentService.pay(first), paymentService.pay(second)))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1().getNewBalance()).isIn(700L, 400L);
                    assertThat(tuple.getT2().getNewBalance()).isIn(700L, 400L);
                })
                .verifyComplete();

        assertThat(currentBalance()).isEqualTo(400L);
    }
}