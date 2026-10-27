package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentStatus;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.model.Account;
import ru.yandex.practicum.payment.repository.AccountRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder().userId(USER_ID).balance(1000L).build();
    }

    @Test
    @DisplayName("getBalance returns balance for user")
    void getBalance_shouldReturnBalance() {
        when(accountRepository.findById(USER_ID)).thenReturn(Mono.just(testAccount));

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(r -> assertThat(r.getBalance()).isEqualTo(1000L))
                .verifyComplete();
    }

    @Test
    @DisplayName("getBalance throws when account not found")
    void getBalance_shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(InsufficientFundsException.class);
                    assertThat(e.getMessage()).contains("not found");
                })
                .verify();
    }

    @Test
    @DisplayName("Successful payment decreases balance")
    void pay_shouldDecreaseBalance() {
        when(accountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Mono.just(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        PaymentRequest request = new PaymentRequest().userId(USER_ID).amount(300L);

        StepVerifier.create(paymentService.pay(request))
                .assertNext(r -> {
                    assertThat(r.getUserId()).isEqualTo(USER_ID);
                    assertThat(r.getAmount()).isEqualTo(300L);
                    assertThat(r.getNewBalance()).isEqualTo(700L);
                    assertThat(r.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Payment fails when insufficient funds")
    void pay_shouldFailWhenInsufficientFunds() {
        when(accountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Mono.just(testAccount));

        PaymentRequest request = new PaymentRequest().userId(USER_ID).amount(5000L);

        StepVerifier.create(paymentService.pay(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(InsufficientFundsException.class);
                    assertThat(e.getMessage()).contains("Insufficient funds");
                })
                .verify();
    }

    @Test
    @DisplayName("Payment fails when account not found")
    void pay_shouldFailWhenAccountNotFound() {
        when(accountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Mono.empty());

        PaymentRequest request = new PaymentRequest().userId(USER_ID).amount(100L);

        StepVerifier.create(paymentService.pay(request))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(InsufficientFundsException.class);
                    assertThat(e.getMessage()).contains("not found");
                })
                .verify();
    }

    @Test
    @DisplayName("Payment with exact balance succeeds and leaves 0")
    void pay_shouldSucceedWhenAmountEqualsBalance() {
        when(accountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Mono.just(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        PaymentRequest request = new PaymentRequest().userId(USER_ID).amount(1000L);

        StepVerifier.create(paymentService.pay(request))
                .assertNext(r -> {
                    assertThat(r.getNewBalance()).isEqualTo(0L);
                    assertThat(r.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                })
                .verifyComplete();
    }
}