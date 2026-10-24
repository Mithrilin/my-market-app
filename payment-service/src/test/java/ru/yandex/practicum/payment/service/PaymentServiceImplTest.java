package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.config.PaymentProperties;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentStatus;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceImplTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(new PaymentProperties(1000L));
    }

    @Test
    @DisplayName("getBalance returns initial balance")
    void getBalance_shouldReturnInitialBalance() {
        StepVerifier.create(paymentService.getBalance())
                .assertNext(r -> assertThat(r.getBalance()).isEqualTo(1000L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Successful payment decreases balance")
    void pay_shouldDecreaseBalance() {
        PaymentRequest request = new PaymentRequest().amount(300L);

        StepVerifier.create(paymentService.pay(request))
                .assertNext(r -> {
                    assertThat(r.getAmount()).isEqualTo(300L);
                    assertThat(r.getNewBalance()).isEqualTo(700L);
                    assertThat(r.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .assertNext(r -> assertThat(r.getBalance()).isEqualTo(700L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Payment fails when insufficient funds")
    void pay_shouldFailWhenInsufficientFunds() {
        PaymentRequest request = new PaymentRequest().amount(5000L);

        StepVerifier.create(paymentService.pay(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(InsufficientFundsException.class);
                    assertThat(e.getMessage()).contains("Insufficient funds");
                })
                .verify();

        StepVerifier.create(paymentService.getBalance())
                .assertNext(r -> assertThat(r.getBalance()).isEqualTo(1000L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Payment with exact balance succeeds and leaves 0")
    void pay_shouldSucceedWhenAmountEqualsBalance() {
        PaymentRequest request = new PaymentRequest().amount(1000L);

        StepVerifier.create(paymentService.pay(request))
                .assertNext(r -> {
                    assertThat(r.getNewBalance()).isEqualTo(0L);
                    assertThat(r.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Consecutive payments accumulate correctly")
    void pay_shouldHandleMultiplePayments() {
        StepVerifier.create(paymentService.pay(new PaymentRequest().amount(100L)))
                .assertNext(r -> assertThat(r.getNewBalance()).isEqualTo(900L))
                .verifyComplete();

        StepVerifier.create(paymentService.pay(new PaymentRequest().amount(200L)))
                .assertNext(r -> assertThat(r.getNewBalance()).isEqualTo(700L))
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .assertNext(r -> assertThat(r.getBalance()).isEqualTo(700L))
                .verifyComplete();
    }
}