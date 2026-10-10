package ru.yandex.practicum.my_market_app.dto;

import ru.yandex.practicum.my_market_app.enums.Sort;

public class RequestDefaults {
    public static Sort getSortOrDefault(Sort sort) {
        return sort == null ? Sort.NO : sort;
    }

    public static Integer getPageNumberOrDefault(Integer pageNumber) {
        return pageNumber == null ? 1 : pageNumber;
    }

    public static Integer getPageSizeOrDefault(Integer pageSize) {
        return pageSize == null ? 5 : pageSize;
    }
}
