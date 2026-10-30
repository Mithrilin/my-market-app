package ru.yandex.practicum.market.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.model.Item;
import ru.yandex.practicum.market.model.ItemWithCount;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            ORDER BY i.id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemWithCount> findAllOrderById(Long userId, int limit, int offset);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            ORDER BY i.title ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemWithCount> findAllOrderByTitle(Long userId, int limit, int offset);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            ORDER BY i.price ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemWithCount> findAllOrderByPrice(Long userId, int limit, int offset);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY i.id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemWithCount> searchOrderById(Long userId, String search, int limit, int offset);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY i.title ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemWithCount> searchOrderByTitle(Long userId, String search, int limit, int offset);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY i.price ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemWithCount> searchOrderByPrice(Long userId, String search, int limit, int offset);

    @Query("""
            SELECT COUNT(*) FROM items
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Mono<Long> countSearch(String search);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id AND ci.user_id = :userId
            WHERE i.id = :id
            """)
    Mono<ItemWithCount> findByIdWithCount(Long userId, Long id);

    @Query("""
            SELECT i.id, i.title, i.description, i.img_path, i.price, ci.count AS count
            FROM items i
            INNER JOIN cart_items ci ON ci.item_id = i.id
            WHERE ci.user_id = :userId AND ci.count >= :minCount
            """)
    Flux<ItemWithCount> findCartItemsByUserId(Long userId, Integer minCount);
}