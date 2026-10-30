package ru.yandex.practicum.payment.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.generated.BalanceResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentStatus;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.repository.AccountRepository;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final AccountRepository accountRepository;

    @Override
    public Mono<BalanceResponse> getBalance(Long userId) {
        return accountRepository.findById(userId)
                .switchIfEmpty(Mono.error(new InsufficientFundsException(
                        String.format("Account for user %d not found", userId))))
                .map(account -> new BalanceResponse().balance(account.getBalance()));
    }

    @Override
    @Transactional
    public Mono<PaymentResponse> pay(PaymentRequest request) {
        return accountRepository.findByUserIdForUpdate(request.getUserId())
                .switchIfEmpty(Mono.error(new InsufficientFundsException(
                        String.format("Account for user %d not found", request.getUserId()))))
                .flatMap(account -> {
                    long amount = request.getAmount();
                    if (account.getBalance() < amount) {
                        return Mono.error(new InsufficientFundsException(
                                String.format("Insufficient funds: balance = %d, required = %d",
                                        account.getBalance(), amount)));
                    }
                    account.setBalance(account.getBalance() - amount);
                    return accountRepository.save(account);
                })
                .map(saved -> {
                    log.info("Payment succeeded: user {}, amount {}, new balance {}",
                            saved.getUserId(), request.getAmount(), saved.getBalance());
                    return new PaymentResponse()
                            .userId(saved.getUserId())
                            .amount(request.getAmount())
                            .newBalance(saved.getBalance())
                            .status(PaymentStatus.SUCCESS);
                });
    }
}