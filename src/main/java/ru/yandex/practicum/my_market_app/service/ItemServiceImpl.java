package ru.yandex.practicum.my_market_app.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.my_market_app.dto.ItemCartListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.dto.Paging;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.exception.BadRequestException;
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

    public static final Integer MIN_COUNT_ITEMS_IN_CART = 1;
    private static final long PLUG_ID = -1L;
    private static final ItemDto PLUG = ItemDto.builder()
            .id(PLUG_ID)
            .title("plug")
            .build();

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

    @Override
    @Transactional
    public ItemDto changeItemCountFromItemPage(Long itemId, Action action) {
        if (action.equals(Action.DELETE)) {
            throw new BadRequestException("Action cannot be equal DELETE.");
        }
        Item returnedItem = changeItemCount(itemId, action);
        ItemDto itemDto = itemMapper.toItemDto(returnedItem);
        log.info("Count of Item with ID {} has been changed from item page. New count = {}.", itemId, itemDto.count());
        return itemDto;
    }

    @Override
    public ItemCartListResponse getItemCartList() {
        ItemCartListResponse itemCartListResponse = getItemCartListResponse();
        log.info("ItemCartList with size = {} and total price {} has been returned",
                itemCartListResponse.itemDtoList().size(), itemCartListResponse.total());
        return itemCartListResponse;
    }

    @Override
    @Transactional
    public ItemCartListResponse changeItemCountFromCartPage(Long itemId, Action action) {
        Item returnedItem = changeItemCount(itemId, action);
        ItemCartListResponse itemCartListResponse = getItemCartListResponse();
        log.info("Count of Item with ID {} has been changed from cart page. New count = {}.",
                itemId, returnedItem.getCount());
        return itemCartListResponse;
    }

    private ItemCartListResponse getItemCartListResponse() {
        List<Item> itemList = itemRepository.findByCountGreaterThanEqual(MIN_COUNT_ITEMS_IN_CART);
        long total = 0L;
        for (Item item : itemList) {
            total = total + item.getPrice() * item.getCount();
        }
        List<ItemDto> itemDtoList = itemMapper.toItemDtoList(itemList);
        return new ItemCartListResponse(itemDtoList, total);
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
        int rowSize = 3;
        List<List<ItemDto>> chunkList = new ArrayList<>();
        for (int i = 0; i < itemDtoList.size(); i += rowSize) {
            List<ItemDto> chunk = new ArrayList<>(
                    itemDtoList.subList(i, Math.min(i + rowSize, itemDtoList.size()))
            );
            while (chunk.size() < rowSize) {
                chunk.add(PLUG);
            }
            chunkList.add(chunk);
        }
        return chunkList;
    }

    private Item changeItemCount(Long itemId, Action action) {
        Item item = findItemById(itemId);
        Integer count = item.getCount();
        if (action.equals(Action.MINUS)) {
            if (count == 0) {
                throw new BadRequestException("Count cannot be less than 0");
            }
            --count;
        } else if (action.equals(Action.PLUS)){
            ++count;
        } else {
            count = 0;
        }
        item.setCount(count);
        return itemRepository.save(item);
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(String.format("Item with ID = %d not found", itemId)));
    }
}
