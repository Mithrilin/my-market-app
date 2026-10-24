package ru.yandex.practicum.market.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.CartPageResponse;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.ItemListResponse;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.enums.Action;

public interface ItemService {

    Mono<ItemDto> getItemById(Long itemId);

    Mono<ItemListResponse> getItemList(ItemListSearchRequest request);

    Mono<ItemDto> changeItemCountFromItemPage(Long itemId, Action action);

    Mono<CartPageResponse> getItemCartList();

    Mono<CartPageResponse> changeItemCountFromCartPage(Long itemId, Action action);
}
