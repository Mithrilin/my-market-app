package ru.yandex.practicum.market.dto;

import lombok.Builder;

@Builder
public record Paging(

        int pageSize,

        int pageNumber,

        boolean hasPrevious,

        boolean hasNext
) {
}
