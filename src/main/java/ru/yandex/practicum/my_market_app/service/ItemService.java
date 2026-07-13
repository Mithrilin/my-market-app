package ru.yandex.practicum.my_market_app.service;

import ru.yandex.practicum.my_market_app.dto.ItemCartListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.enums.Action;

public interface ItemService {

    ItemDto getItemById(Long itemId);

    ItemListResponse getItemList(ItemListSearchRequest itemListSearchRequest);

    ItemDto changeItemCountFromItemPage(Long itemId, Action action);

    ItemCartListResponse getItemCartList();

    ItemCartListResponse changeItemCountFromCartPage(Long itemId, Action action);
}
