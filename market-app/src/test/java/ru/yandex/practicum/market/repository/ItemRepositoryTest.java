package ru.yandex.practicum.market.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Search by title ignore case")
    void searchOrderById_shouldFindItemsByTitleIgnoreCase() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple", "Red fruit", 100L, 5),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 150L, 3),
                TestDataFactory.createTestItem(null, "Orange", "Citrus fruit", 120L, 2)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderById("apple", 10, 0))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Apple"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Search by description")
    void searchOrderById_shouldFindItemsByDescription() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Phone", "Smartphone with large screen", 800L, 5),
                TestDataFactory.createTestItem(null, "TV", "Large screen television", 1200L, 3)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderById("screen", 10, 0))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Search by partial title")
    void searchOrderById_shouldFindByPartialTitle() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Laptop", "Portable computer", 1000L, 5),
                TestDataFactory.createTestItem(null, "Tablet", "Mobile device", 500L, 3),
                TestDataFactory.createTestItem(null, "Desktop", "Stationary computer", 1500L, 2)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderById("lap", 10, 0))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Laptop"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Search returns empty when no matches")
    void searchOrderById_shouldReturnEmptyWhenNoMatches() {
        itemRepository.save(TestDataFactory.createTestItem(
                null, "Laptop", "Portable computer", 1000L, 5)).block();

        StepVerifier.create(itemRepository.searchOrderById("nonexistent", 10, 0))
                .verifyComplete();

        StepVerifier.create(itemRepository.countSearch("nonexistent"))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Search with sort by title")
    void searchOrderByTitle_shouldSortByTitle() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple juice", "From apple", 100L, 1),
                TestDataFactory.createTestItem(null, "Apple pie", "With apple", 50L, 1),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 200L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderByTitle("apple", 10, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Apple juice"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Apple pie"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Search with sort by price")
    void searchOrderByPrice_shouldSortByPrice() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple juice", "From apple", 100L, 1),
                TestDataFactory.createTestItem(null, "Apple pie", "With apple", 50L, 1),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 200L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderByPrice("apple", 10, 0))
                .assertNext(i -> {
                    assertThat(i.getTitle()).isEqualTo("Apple pie");
                    assertThat(i.getPrice()).isEqualTo(50L);
                })
                .assertNext(i -> {
                    assertThat(i.getTitle()).isEqualTo("Apple juice");
                    assertThat(i.getPrice()).isEqualTo(100L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Search with pagination — first page")
    void searchOrderById_shouldRespectLimitAndOffset() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item A", "desc", 100L, 1),
                TestDataFactory.createTestItem(null, "Item B", "desc", 200L, 1),
                TestDataFactory.createTestItem(null, "Item C", "desc", 300L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.searchOrderById("item", 2, 0))
                .expectNextCount(2)
                .verifyComplete();

        StepVerifier.create(itemRepository.searchOrderById("item", 2, 2))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item C"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderById returns items sorted by id")
    void findAllOrderById_shouldSortById() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L, 1),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L, 1),
                TestDataFactory.createTestItem(null, "Item 3", "desc", 300L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderById(10, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 2"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 3"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderByTitle returns items sorted by title")
    void findAllOrderByTitle_shouldSortByTitle() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Banana", "Yellow", 100L, 1),
                TestDataFactory.createTestItem(null, "Apple", "Red", 200L, 1),
                TestDataFactory.createTestItem(null, "Cherry", "Dark red", 50L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderByTitle(10, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Apple"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Banana"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Cherry"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderByPrice returns items sorted by price")
    void findAllOrderByPrice_shouldSortByPrice() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Banana", "Yellow", 100L, 1),
                TestDataFactory.createTestItem(null, "Apple", "Red", 200L, 1),
                TestDataFactory.createTestItem(null, "Cherry", "Dark red", 50L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderByPrice(10, 0))
                .assertNext(i -> assertThat(i.getPrice()).isEqualTo(50L))
                .assertNext(i -> assertThat(i.getPrice()).isEqualTo(100L))
                .assertNext(i -> assertThat(i.getPrice()).isEqualTo(200L))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderById respects LIMIT/OFFSET")
    void findAllOrderById_shouldRespectLimitAndOffset() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L, 1),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L, 1),
                TestDataFactory.createTestItem(null, "Item 3", "desc", 300L, 1),
                TestDataFactory.createTestItem(null, "Item 4", "desc", 400L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderById(2, 0))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 2"))
                .verifyComplete();

        StepVerifier.create(itemRepository.findAllOrderById(2, 2))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 3"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 4"))
                .verifyComplete();
    }

    @Test
    @DisplayName("findAllOrderById returns empty when offset beyond size")
    void findAllOrderById_shouldReturnEmptyWhenOffsetBeyondSize() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L, 1),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.findAllOrderById(10, 100))
                .verifyComplete();
    }

    @Test
    @DisplayName("Find items with count >= 1")
    void findByCountGreaterThanEqual_shouldFindItemsInCart() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "Desc 1", 100L, 3),
                TestDataFactory.createTestItem(null, "Item 2", "Desc 2", 200L, 1),
                TestDataFactory.createTestItem(null, "Item 3", "Desc 3", 300L, 0)
        )).blockLast();

        StepVerifier.create(itemRepository.findByCountGreaterThanEqual(1))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Find items with count >= 5")
    void findByCountGreaterThanEqual_shouldFindItemsWithHighCount() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "Desc 1", 100L, 5),
                TestDataFactory.createTestItem(null, "Item 2", "Desc 2", 200L, 3),
                TestDataFactory.createTestItem(null, "Item 3", "Desc 3", 300L, 10)
        )).blockLast();

        StepVerifier.create(itemRepository.findByCountGreaterThanEqual(5))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 3"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Find items when no matches")
    void findByCountGreaterThanEqual_shouldReturnEmptyWhenNoMatches() {
        itemRepository.save(TestDataFactory.createTestItem(
                null, "Item 1", "Desc 1", 100L, 0)).block();

        StepVerifier.create(itemRepository.findByCountGreaterThanEqual(1))
                .verifyComplete();
    }

    @Test
    @DisplayName("count returns total number of items")
    void count_shouldReturnTotalNumberOfItems() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Item 1", "desc", 100L, 1),
                TestDataFactory.createTestItem(null, "Item 2", "desc", 200L, 1),
                TestDataFactory.createTestItem(null, "Item 3", "desc", 300L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.count())
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("count returns 0 for empty table")
    void count_shouldReturnZeroForEmptyTable() {
        StepVerifier.create(itemRepository.count())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("countSearch counts only matching items")
    void countSearch_shouldCountMatchingItems() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Apple juice", "From apple", 100L, 1),
                TestDataFactory.createTestItem(null, "Apple pie", "With apple", 50L, 1),
                TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 200L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.countSearch("apple"))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("countSearch matches description too")
    void countSearch_shouldMatchDescription() {
        itemRepository.saveAll(List.of(
                TestDataFactory.createTestItem(null, "Phone", "Smartphone with large screen", 800L, 1),
                TestDataFactory.createTestItem(null, "TV", "Large screen television", 1200L, 1),
                TestDataFactory.createTestItem(null, "Book", "Just a book", 100L, 1)
        )).blockLast();

        StepVerifier.create(itemRepository.countSearch("screen"))
                .expectNext(2L)
                .verifyComplete();
    }
}