package ru.yandex.practicum.market.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.CartPageResponse;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.ItemListResponse;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.enums.Action;

public interface ItemService {

    Mono<ItemDto> getItemById(Long itemId, Long userId);

    Mono<ItemListResponse> getItemList(ItemListSearchRequest request, Long userId);

    Mono<ItemDto> changeItemCountFromItemPage(Long itemId, Action action, Long userId);

    Mono<CartPageResponse> getItemCartList(Long userId);

    Mono<CartPageResponse> changeItemCountFromCartPage(Long itemId, Action action, Long userId);
}