package ru.yandex.practicum.market.util;

import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.enums.Sort;
import ru.yandex.practicum.market.model.Item;
import ru.yandex.practicum.market.model.ItemWithCount;
import ru.yandex.practicum.market.model.Order;
import ru.yandex.practicum.market.model.OrderItem;
import ru.yandex.practicum.market.model.User;

import java.util.List;

public class TestDataFactory {

    public static Item createTestItem(Long id, String title, String description, Long price) {
        return Item.builder()
                .id(id)
                .title(title)
                .description(description)
                .imgPath("/images/test.jpg")
                .price(price)
                .build();
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

    public static ItemWithCount createTestItemWithCount(Long id, String title, String description,
                                                        Long price, Integer count) {
        return ItemWithCount.builder()
                .id(id)
                .title(title)
                .description(description)
                .imgPath("/images/test.jpg")
                .price(price)
                .count(count)
                .build();
    }

    public static List<ItemWithCount> createTestItemWithCountList(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> createTestItemWithCount((long) i, "Item " + i,
                        "Description for item " + i, 100L * i, i))
                .collect(java.util.stream.Collectors.toList());
    }

    public static User createTestUser(Long id, String username, String password, String role) {
        return User.builder()
                .id(id)
                .username(username)
                .password(password)
                .role(role)
                .build();
    }

    public static ItemListSearchRequest createDefaultSearchRequest() {
        return new ItemListSearchRequest(null, Sort.NO, 1, 5);
    }

    public static ItemListSearchRequest createSearchRequest(String search, Sort sort, Integer page, Integer size) {
        return new ItemListSearchRequest(search, sort, page, size);
    }

    public static OrderItem createTestOrderItem(Long id, Long orderId, String title, Long price, Integer count) {
        return OrderItem.builder()
                .id(id)
                .orderId(orderId)
                .title(title)
                .price(price)
                .count(count)
                .build();
    }

    public static Order createTestOrder(Long id, Long userId, Long totalSum) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .totalSum(totalSum)
                .build();
    }
}