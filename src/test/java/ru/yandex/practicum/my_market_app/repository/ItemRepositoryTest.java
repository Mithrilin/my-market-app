package ru.yandex.practicum.my_market_app.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;

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
    @DisplayName("Successful Item search by title or description")
    void search_shouldFindItemsByTitleOrDescriptionIgnoreCase() {
        Item item1 = TestDataFactory.createTestItem(null, "Apple", "Red fruit", 100L, 5);
        Item item2 = TestDataFactory.createTestItem(null, "Banana", "Yellow fruit", 150L, 3);
        Item item3 = TestDataFactory.createTestItem(null, "Orange", "Citrus fruit", 120L, 2);

        itemRepository.saveAll(List.of(item1, item2, item3)).blockLast();

        StepVerifier.create(itemRepository.search("apple", 10, 0))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Apple"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Successful Item search based on the title")
    void search_shouldFindItemsByPartialTitle() {
        Item item1 = TestDataFactory.createTestItem(null, "Laptop", "Portable computer", 1000L, 5);
        Item item2 = TestDataFactory.createTestItem(null, "Tablet", "Mobile device", 500L, 3);
        Item item3 = TestDataFactory.createTestItem(null, "Desktop", "Stationary computer", 1500L, 2);

        itemRepository.saveAll(List.of(item1, item2, item3)).blockLast();

        StepVerifier.create(itemRepository.search("lap", 10, 0))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Laptop"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Successful Item search based on the description")
    void search_shouldFindItemsByPartialDescription() {
        Item item1 = TestDataFactory.createTestItem(null, "Phone", "Smartphone with large screen", 800L, 5);
        Item item2 = TestDataFactory.createTestItem(null, "TV", "Large screen television", 1200L, 3);

        itemRepository.saveAll(List.of(item1, item2)).blockLast();

        StepVerifier.create(itemRepository.search("screen", 10, 0))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Item search when no matches")
    void search_shouldReturnEmptyWhenNoMatches() {
        Item item = TestDataFactory.createTestItem(null, "Laptop", "Portable computer", 1000L, 5);
        itemRepository.save(item).block();

        StepVerifier.create(itemRepository.search("nonexistent", 10, 0))
                .verifyComplete();

        StepVerifier.create(itemRepository.countSearch("nonexistent"))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Getting items with count greater than equal")
    void findByCountGreaterThanEqual_shouldFindItemsWithCountGreaterThanEqual() {
        Item item1 = TestDataFactory.createTestItem(null, "Item 1", "Desc 1", 100L, 3);
        Item item2 = TestDataFactory.createTestItem(null, "Item 2", "Desc 2", 200L, 1);
        Item item3 = TestDataFactory.createTestItem(null, "Item 3", "Desc 3", 300L, 0);

        itemRepository.saveAll(List.of(item1, item2, item3)).blockLast();

        StepVerifier.create(itemRepository.findByCountGreaterThanEqual(1))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 1"))
                .assertNext(i -> assertThat(i.getTitle()).isEqualTo("Item 2"))
                .verifyComplete();
    }
}