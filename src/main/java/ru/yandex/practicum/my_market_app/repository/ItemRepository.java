package ru.yandex.practicum.my_market_app.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.model.Item;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    @Query("""
            SELECT * FROM items
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY id
            LIMIT :limit OFFSET :offset
            """)
    Flux<Item> search(String search, int limit, int offset);

    @Query("""
            SELECT COUNT(*) FROM items
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Mono<Long> countSearch(String search);

    Flux<Item> findByCountGreaterThanEqual(Integer minCount);

    Mono<Long> countByCountGreaterThanEqual(Integer minCount);
}
