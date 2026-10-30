package ru.yandex.practicum.payment.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.Account;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {

    @Query("SELECT * FROM accounts WHERE user_id = :userId FOR UPDATE")
    Mono<Account> findByUserIdForUpdate(Long userId);
}