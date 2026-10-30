package ru.yandex.practicum.market.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.config.R2dbcInitConfig;
import ru.yandex.practicum.market.model.User;
import ru.yandex.practicum.market.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(R2dbcInitConfig.class)
@ActiveProfiles("test")
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        databaseClient.sql("DELETE FROM cart_items").then().block();
        databaseClient.sql("DELETE FROM order_items").then().block();
        databaseClient.sql("DELETE FROM orders").then().block();
        itemRepository.deleteAll().block();
        userRepository.deleteAll().block();

        User testUser = userRepository.save(
                TestDataFactory.createTestUser(null, "testuser", "password", "ROLE_USER")
        ).block();
        testUserId = testUser.getId();
    }

    @Test
    @DisplayName("Search by title ignore case")
    void searchOrderById_shouldFindItemsByTitleIgnoreCase() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple", "Red fruit", 100L),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 150L),
                TestDataFactory.createTestItem(null, "Orange", "Citrus fruit", 120L)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderById(testUserId, "apple", 10, 0))
                .assertNext(item -> {
                    assertThat(item.getTitle()).isEqualTo("Apple");
                    assertThat(item.getCount()).isZero();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Search by description")
    void searchOrderById_shouldFindItemsByDescription() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Phone", "Smartphone with large screen", 800L),
                TestDataFactory.createTestItem(null, "TV", "Large screen television", 1200L)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderById(testUserId, "screen", 10, 0))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Search with sort by title")
    void searchOrderByTitle_shouldSortByTitle() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple juice", "From apple", 100L),
                TestDataFactory.createTestItem(null, "Apple pie", "With apple", 50L),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 200L)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderByTitle(testUserId, "apple", 10, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Apple juice"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Apple pie"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderById returns items sorted by id with count from cart")
    void findAllOrderById_shouldSortByIdAndIncludeCartCount() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L),
                TestDataFactory.createTestItem(null, "Item 3", "desc", 300L)
        )).blockLast();

        Long firstItemId = itemRepository.findAllOrderById(testUserId, 10, 0)
                .collectList().block().getFirst().getId();

        databaseClient.sql("INSERT INTO cart_items (user_id, item_id, \"count\") VALUES ($1, $2, $3)")
                .bind(0, testUserId).bind(1, firstItemId).bind(2, 3)
                .then().block();

        StepVerifier.create(itemRepository.findAllOrderById(testUserId, 10, 0))
                .assertNext(item -> {
                    assertThat(item.getTitle()).isEqualTo("Item 1");
                    assertThat(item.getCount()).isEqualTo(3);
                })
                .assertNext(item -> {
                    assertThat(item.getTitle()).isEqualTo("Item 2");
                    assertThat(item.getCount()).isZero();
                })
                .assertNext(item -> {
                    assertThat(item.getTitle()).isEqualTo("Item 3");
                    assertThat(item.getCount()).isZero();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderByTitle returns items sorted by title")
    void findAllOrderByTitle_shouldSortByTitle() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Banana", "Yellow", 100L),
                TestDataFactory.createTestItem(null, "Apple", "Red", 200L),
                TestDataFactory.createTestItem(null, "Cherry", "Dark red", 50L)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderByTitle(testUserId, 10, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Apple"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Banana"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Cherry"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderByPrice returns items sorted by price")
    void findAllOrderByPrice_shouldSortByPrice() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Banana", "Yellow", 100L),
                TestDataFactory.createTestItem(null, "Apple", "Red", 200L),
                TestDataFactory.createTestItem(null, "Cherry", "Dark red", 50L)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderByPrice(testUserId, 10, 0))
                .assertNext(i -> assertThat(i.getPrice()).isEqualTo(50L))
                .assertNext(i -> assertThat(i.getPrice()).isEqualTo(100L))
                .assertNext(i -> assertThat(i.getPrice()).isEqualTo(200L))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderById respects LIMIT/OFFSET")
    void findAllOrderById_shouldRespectLimitAndOffset() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L),
                TestDataFactory.createTestItem(null, "Item 3", "desc", 300L),
                TestDataFactory.createTestItem(null, "Item 4", "desc", 400L)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderById(testUserId, 2, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 2"))
                .verifyComplete();

        StepVerifier.create(itemRepository.findAllOrderById(testUserId, 2, 2))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 3"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 4"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findCartItemsByUserId returns only items with count >= 1 for user")
    void findCartItemsByUserId_shouldReturnOnlyCartItems() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "Desc 1", 100L),
                TestDataFactory.createTestItem(null, "Item 2", "Desc 2", 200L),
                TestDataFactory.createTestItem(null, "Item 3", "Desc 3", 300L)
        )).blockLast();

        List<Long> itemIds = itemRepository.findAllOrderById(testUserId, 10, 0)
                .collectList().block().stream().map(i -> i.getId()).toList();

        databaseClient.sql("INSERT INTO cart_items (user_id, item_id, \"count\") VALUES ($1, $2, $3)")
                .bind(0, testUserId).bind(1, itemIds.get(0)).bind(2, 2)
                .then().block();
        databaseClient.sql("INSERT INTO cart_items (user_id, item_id, \"count\") VALUES ($1, $2, $3)")
                .bind(0, testUserId).bind(1, itemIds.get(1)).bind(2, 1)
                .then().block();

        StepVerifier.create(itemRepository.findCartItemsByUserId(testUserId, 1))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("count returns total number of items")
    void count_shouldReturnTotalNumberOfItems() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L),
                TestDataFactory.createTestItem(null, "Item 3", "desc", 300L)
        )).blockLast();

        StepVerifier.create(itemRepository.count())
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("countSearch counts only matching items")
    void countSearch_shouldCountMatchingItems() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple juice", "From apple", 100L),
                TestDataFactory.createTestItem(null, "Apple pie", "With apple", 50L),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 200L)
        )).blockLast();

        StepVerifier.create(itemRepository.countSearch("apple"))
                .expectNext(2L)
                .verifyComplete();
    }
}