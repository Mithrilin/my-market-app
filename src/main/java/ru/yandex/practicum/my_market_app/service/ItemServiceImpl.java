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

    @Override
    public ItemListResponse getItemList(ItemListSearchRequest itemListSearchRequest) {
        Page<Item> itemPage = getItemPage(itemListSearchRequest);
        List<Item> itemList = itemPage.getContent();
        List<ItemDto> itemDtoList = itemMapper.toItemDtoList(itemList);
        Paging paging = new Paging(itemListSearchRequest.pageSize(), itemListSearchRequest.pageNumber(),
                itemPage.hasPrevious(), itemPage.hasNext());
        List<List<ItemDto>> splittedItemDtoList = splitIntoChunksOfThree(itemDtoList);
        ItemListResponse itemListResponse = new ItemListResponse(splittedItemDtoList, paging);
        log.info("Item page with number = {} size = {} has been returned.", itemListSearchRequest.pageNumber(),
                itemListSearchRequest.pageSize());
        return itemListResponse;
    }
    private Page<Item> getItemPage(ItemListSearchRequest itemListSearchRequest) {
        Sort sort = switch (itemListSearchRequest.sort()) {
            case ALPHA -> Sort.by(Sort.Direction.ASC, "title");
            case PRICE -> Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.by(Sort.Direction.ASC, "id");
        };
        PageRequest pageRequest = PageRequest.of(itemListSearchRequest.pageNumber() - 1,
                itemListSearchRequest.pageSize(), sort);
        String search = itemListSearchRequest.search();
        Page<Item> itemPage;
        if (search == null) {
            itemPage = itemRepository.findAll(pageRequest);
        } else {
            itemPage = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageRequest);
        }
        return itemPage;
    }

    private List<List<ItemDto>> splitIntoChunksOfThree(List<ItemDto> itemDtoList) {
        List<List<ItemDto>> chunkList = new ArrayList<>();
        for (int i = 0; i < itemDtoList.size(); i += 3) {
            List<ItemDto> chunk = new ArrayList<>();
            int end = Math.min(i + 3, itemDtoList.size());
            for (int j = i; j < end; j++) {
                chunk.add(itemDtoList.get(j));
            }
            for (int j = chunk.size(); j < 3; j++) {
                chunk.add(ItemDto.builder()
                        .id(-1L)
                        .title("plug")
                        .build());
            }
            chunkList.add(chunk);
        }
        return chunkList;
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(String.format("Item with ID = %d not found", itemId)));
    }
}
