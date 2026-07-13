package ru.yandex.practicum.my_market_app.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.dto.Paging;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.exception.NotFoundException;
import ru.yandex.practicum.my_market_app.mapper.ItemMapper;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    private final ItemMapper itemMapper;

    @Override
    public ItemDto getItemById(Long id) {
        Item item = findItemById(id);
        ItemDto itemDto = itemMapper.toItemDto(item);
        log.info("Item with ID = {} returned.", itemDto.id());
        return itemDto;
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(String.format("Item with ID = %d not found", itemId)));
    }
}
