package ru.yandex.practicum.my_market_app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.enums.Sort;

@Builder
public record ItemChangeCountRequest(

        @NotNull(message = "Id cannot be null.")
        Long id,

        @Size(max = 100, message = "Search query cannot exceed 100 characters")
        String search,

        Sort sort,

        @Positive(message = "Page number must be greater than 0")
        Integer pageNumber,

        @Positive(message = "Page size must be greater than 0")
        Integer pageSize,

        @NotNull(message = "Action cannot be null.")
        Action action
) {

    public ItemChangeCountRequest {

        if (sort == null) {
            sort = Sort.NO;
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }
        if (pageSize == null) {
            pageSize = 5;
        }
    }
}
