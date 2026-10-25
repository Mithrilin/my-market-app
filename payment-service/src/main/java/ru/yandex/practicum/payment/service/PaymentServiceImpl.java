package ru.yandex.practicum.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.PaymentProperties;
import ru.yandex.practicum.payment.dto.generated.BalanceResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentRequest;
import ru.yandex.practicum.payment.dto.generated.PaymentResponse;
import ru.yandex.practicum.payment.dto.generated.PaymentStatus;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final AtomicLong balance;

    public PaymentServiceImpl(PaymentProperties properties) {
        Long initial = properties.initialBalance() == null ? 0L : properties.initialBalance();
        this.balance = new AtomicLong(initial);
        log.info("Payment service initialized with initial balance = {}", initial);
    }

    @Override
    public Mono<BalanceResponse> getBalance() {
        return Mono.fromSupplier(() -> new BalanceResponse().balance(balance.get()));
    }

    @Override
    public Mono<PaymentResponse> pay(PaymentRequest request) {
        return Mono.fromCallable(() -> deduct(request));
    }

    private PaymentResponse deduct(PaymentRequest request) {
        long amount = request.getAmount();
        while (true) {
            long current = balance.get();
            if (current < amount) {
                throw new InsufficientFundsException(
                        String.format("Insufficient funds: balance = %d, required = %d", current, amount));
            }
            long newBalance = current - amount;
            if (balance.compareAndSet(current, newBalance)) {
                log.info("Payment succeeded: {} deducted, new balance = {}", amount, newBalance);
                return new PaymentResponse()
                        .amount(amount)
                        .newBalance(newBalance)
                        .status(PaymentStatus.SUCCESS);
            }
        }
    }
}
