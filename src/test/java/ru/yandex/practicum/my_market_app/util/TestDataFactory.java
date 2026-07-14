package ru.yandex.practicum.my_market_app.util;

import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.enums.Sort;
import ru.yandex.practicum.my_market_app.model.Item;

import java.util.List;

public class TestDataFactory {

    public static Item createTestItem(Long id, String title, String description, Long price, Integer count) {
        return Item.builder()
                .id(id)
                .title(title)
                .description(description)
                .imgPath("/images/test.jpg")
                .price(price)
                .count(count)
                .build();
    }

    public static Item createTestItemWithId(Long id) {
        return createTestItem(id, "Test Item " + id, "Description for item " + id, 100L * id, id.intValue());
    }

    public static List<Item> createTestItemList(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> createTestItemWithId((long) i))
                .collect(java.util.stream.Collectors.toList());
    }

    public static ItemDto createTestItemDto(Long id, String title, Long price, Integer count) {
        return ItemDto.builder()
                .id(id)
                .title(title)
                .description("Description for " + title)
                .imgPath("/images/test.jpg")
                .price(price)
                .count(count)
                .build();
    }

    public static ItemListSearchRequest createDefaultSearchRequest() {
        return new ItemListSearchRequest(null, Sort.NO, 1, 5);
    }

    public static ItemListSearchRequest createSearchRequest(String search, Sort sort, Integer page, Integer size) {
        return new ItemListSearchRequest(search, sort, page, size);
    }
}