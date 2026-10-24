package ru.yandex.practicum.market.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import ru.yandex.practicum.market.enums.Sort;

@Builder
public record ItemListSearchRequest(

        @Size(max = 100, message = "Search query cannot exceed 100 characters")
        String search,

        Sort sort,

        @Positive(message = "Page number must be greater than 0")
        Integer pageNumber,

        @Positive(message = "Page size must be greater than 0")
        Integer pageSize
) {

    public ItemListSearchRequest {

        sort = RequestDefaults.getSortOrDefault(sort);
        pageNumber = RequestDefaults.getPageNumberOrDefault(pageNumber);
        pageSize = RequestDefaults.getPageSizeOrDefault(pageSize);
    }
}
