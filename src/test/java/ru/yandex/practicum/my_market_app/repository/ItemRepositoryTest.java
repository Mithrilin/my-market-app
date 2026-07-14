package ru.yandex.practicum.my_market_app.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("Successful Item search by title or description")
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldFindItemsByTitleOrDescriptionIgnoreCase() {
        Item item1 = TestDataFactory.createTestItem(null, "Apple", "Red fruit",
                100L, 5);
        Item item2 = TestDataFactory.createTestItem(null, "Banana", "Yellow fruit",
                150L, 3);
        Item item3 = TestDataFactory.createTestItem(null, "Orange", "Citrus fruit",
                120L, 2);

        itemRepository.saveAll(List.of(item1, item2, item3));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "apple", "apple", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Successful Item search based on the title")
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldFindItemsByPartialTitle() {
        Item item1 = TestDataFactory.createTestItem(null, "Laptop", "Portable computer",
                1000L, 5);
        Item item2 = TestDataFactory.createTestItem(null, "Tablet", "Mobile device",
                500L, 3);
        Item item3 = TestDataFactory.createTestItem(null, "Desktop", "Stationary computer",
                1500L, 2);

        itemRepository.saveAll(List.of(item1, item2, item3));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "lap", "lap", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Successful Item search based on the description")
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldFindItemsByPartialDescription() {
        Item item1 = TestDataFactory.createTestItem(null, "Phone", "Smartphone with large screen",
                800L, 5);
        Item item2 = TestDataFactory.createTestItem(null, "TV", "Large screen television",
                1200L, 3);

        itemRepository.saveAll(List.of(item1, item2));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "screen", "screen", pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Item search when no matches")
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldReturnEmptyPageWhenNoMatches() {
        Item item = TestDataFactory.createTestItem(null, "Laptop", "Portable computer",
                1000L, 5);
        itemRepository.save(item);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "nonexistent", "nonexistent", pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Getting item with count greater than equal")
    void findByCountGreaterThanEqual_shouldFindItemsWithCountGreaterThanEqual() {
        Item item1 = TestDataFactory.createTestItem(null, "Item 1", "Desc 1", 100L, 3);
        Item item2 = TestDataFactory.createTestItem(null, "Item 2", "Desc 2", 200L, 1);
        Item item3 = TestDataFactory.createTestItem(null, "Item 3", "Desc 3", 300L, 0);

        itemRepository.saveAll(List.of(item1, item2, item3));

        List<Item> result = itemRepository.findByCountGreaterThanEqual(1);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Item::getTitle)
                .containsExactlyInAnyOrder("Item 1", "Item 2");
    }
}