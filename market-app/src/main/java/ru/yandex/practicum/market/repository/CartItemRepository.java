package ru.yandex.practicum.market.repository;

import lombok.AllArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.model.CartItem;

@Repository
@AllArgsConstructor
public class CartItemRepository {

    private final DatabaseClient databaseClient;

    public Flux<CartItem> findByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT user_id, item_id, "count"
                        FROM cart_items
                        WHERE user_id = $1
                        """)
                .bind(0, userId)
                .map((row, meta) -> CartItem.builder()
                        .userId(row.get("user_id", Long.class))
                        .itemId(row.get("item_id", Long.class))
                        .count(row.get("count", Integer.class))
                        .build())
                .all();
    }

    public Mono<Integer> findCount(Long userId, Long itemId) {
        return databaseClient.sql("""
                        SELECT "count" FROM cart_items
                        WHERE user_id = $1 AND item_id = $2
                        """)
                .bind(0, userId)
                .bind(1, itemId)
                .map((row, meta) -> row.get("count", Integer.class))
                .one()
                .defaultIfEmpty(0);
    }

    public Mono<Void> upsert(Long userId, Long itemId, int count) {
        return databaseClient.sql("""
                        INSERT INTO cart_items (user_id, item_id, "count")
                        VALUES ($1, $2, $3)
                        ON CONFLICT (user_id, item_id) DO UPDATE SET "count" = EXCLUDED."count"
                        """)
                .bind(0, userId)
                .bind(1, itemId)
                .bind(2, count)
                .then();
    }

    public Mono<Void> delete(Long userId, Long itemId) {
        return databaseClient.sql("""
                        DELETE FROM cart_items
                        WHERE user_id = $1 AND item_id = $2
                        """)
                .bind(0, userId)
                .bind(1, itemId)
                .then();
    }

    public Mono<Void> deleteAllByUserId(Long userId) {
        return databaseClient.sql("""
                        DELETE FROM cart_items WHERE user_id = $1
                        """)
                .bind(0, userId)
                .then();
    }
}