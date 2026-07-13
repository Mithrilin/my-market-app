package ru.yandex.practicum.my_market_app.dto;

public record Paging(

        int pageSize,

        int pageNumber,

        boolean hasPrevious,

        boolean hasNext
) {
}
