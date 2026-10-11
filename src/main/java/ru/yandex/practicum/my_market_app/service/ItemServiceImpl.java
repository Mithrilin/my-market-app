package ru.yandex.practicum.my_market_app.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
    private static final ItemDto PLUG = ItemDto.builder().id(PLUG_ID).title("plug").build();

    private final ItemRepository itemRepository;

    private final ItemMapper itemMapper;

    @Override
    public Mono<ItemDto> getItemById(Long id) {
        Mono<ItemDto> itemDtoMono = findItemById(id).map(itemMapper::toItemDto);
        log.info("Item with ID = {} returned.", id);
        return itemDtoMono;
    }

    @Override
    public Mono<ItemListResponse> getItemList(ItemListSearchRequest req) {
        int limit = req.pageSize();
        int offset = (req.pageNumber() - 1) * req.pageSize();
        String search = req.search();

        Flux<Item> itemsFlux = itemRepository.search(search, limit, offset);

        Mono<Long> totalMono = (search == null || search.isBlank())
                ? itemRepository.count()
                : itemRepository.countSearch(search);

        Mono<ItemListResponse> itemListResponseMono = itemsFlux.collectList()
                .zipWith(totalMono)
                .map(tuple -> {
                    List<Item> items = tuple.getT1();
                    long total = tuple.getT2();
                    List<ItemDto> itemDtoList = itemMapper.toItemDtoList(items);
                    List<List<ItemDto>> splittedItemDtoList = splitIntoChunksOfThree(itemDtoList);
                    boolean hasPrev = req.pageNumber() > 1;
                    boolean hasNext = (long) req.pageNumber() * req.pageSize() < total;
                    Paging paging = new Paging(req.pageSize(), req.pageNumber(), hasPrev, hasNext);
                    return new ItemListResponse(splittedItemDtoList, paging);
                });
        log.info("Item page has been returned.");
        return itemListResponseMono;
    }

    @Override
    public Mono<ItemDto> changeItemCountFromItemPage(Long itemId, Action action) {
        if (action == Action.DELETE) {
            return Mono.error(new BadRequestException("Action cannot be equal DELETE."));
        }
        Mono<ItemDto> itemDtoMono = changeItemCount(itemId, action).map(itemMapper::toItemDto);
        log.info("Count of Item with ID {} has been changed from item page.", itemId);
        return itemDtoMono;
    }

    @Override
    public Mono<ItemCartListResponse> getItemCartList() {
        Mono<ItemCartListResponse> itemCartListResponseMono = buildCartResponse();
        log.info("ItemCartList has been returned");
        return itemCartListResponseMono;
    }

    @Override
    public Mono<ItemCartListResponse> changeItemCountFromCartPage(Long itemId, Action action) {
        Mono<ItemCartListResponse> itemCartListResponseMono = changeItemCount(itemId, action).then(buildCartResponse());
        log.info("Count of Item with ID {} has been changed from cart page.", itemId);
        return itemCartListResponseMono;
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

    private Mono<Item> changeItemCount(Long itemId, Action action) {
        return findItemById(itemId).flatMap(item -> {
            Integer count = item.getCount();
            switch (action) {
                case MINUS -> {
                    if (count == 0) {
                        return Mono.error(new BadRequestException("Count cannot be less than 0"));
                    }
                    item.setCount(count - 1);
                }
                case PLUS -> item.setCount(count + 1);
                case DELETE -> item.setCount(0);
            }
            return itemRepository.save(item);
        });
    }

    private Mono<Item> findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(String.format("Item with ID = %d not found", itemId))));
    }

    private Mono<ItemCartListResponse> buildCartResponse() {
        return itemRepository.findByCountGreaterThanEqual(MIN_COUNT_ITEMS_IN_CART)
                .collectList()
                .map(items -> {
                    long total = items.stream()
                            .mapToLong(i -> i.getPrice() * i.getCount())
                            .sum();
                    List<ItemDto> dtos = itemMapper.toItemDtoList(items);
                    return new ItemCartListResponse(dtos, total);
                });
    }
}
