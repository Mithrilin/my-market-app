package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.client.PaymentClient;
import ru.yandex.practicum.market.dto.CartPageResponse;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.ItemListResponse;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.dto.Paging;
import ru.yandex.practicum.market.dto.PaymentStatusDto;
import ru.yandex.practicum.market.enums.Action;
import ru.yandex.practicum.market.enums.Sort;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.NotFoundException;
import ru.yandex.practicum.market.mapper.ItemMapper;
import ru.yandex.practicum.market.model.Item;
import ru.yandex.practicum.market.model.ItemWithCount;
import ru.yandex.practicum.market.repository.CartItemRepository;
import ru.yandex.practicum.market.repository.ItemRepository;

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
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;
    private final PaymentClient paymentClient;
    private final CartCacheService cartCacheService;

    @Override
    @Cacheable(cacheNames = "item", key = "#id + ':' + #userId")
    public Mono<ItemDto> getItemById(Long id, Long userId) {
        Mono<ItemDto> itemDtoMono = itemRepository.findByIdWithCount(userId, id)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(String.format("Item with ID = %d not found", id))))
                .map(itemMapper::toItemDto);
        log.info("Item with ID = {} returned.", id);
        return itemDtoMono;
    }

    @Override
    @Cacheable(cacheNames = "itemList",
            key = "#req.sort() + ':' + #req.search() + ':' + #req.pageNumber() + ':' + #req.pageSize() + ':' + #userId")
    public Mono<ItemListResponse> getItemList(ItemListSearchRequest req, Long userId) {
        int limit = req.pageSize();
        int offset = (req.pageNumber() - 1) * req.pageSize();
        String search = req.search();
        boolean hasSearch = search != null && !search.isBlank();

        Flux<ItemWithCount> itemsFlux = resolveItemsFlux(req.sort(), hasSearch, search, userId, limit, offset);
        Mono<Long> totalMono = hasSearch
                ? itemRepository.countSearch(search)
                : itemRepository.count();

        Mono<ItemListResponse> itemListResponseMono = itemsFlux.collectList()
                .zipWith(totalMono)
                .map(tuple -> {
                    List<ItemWithCount> items = tuple.getT1();
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
    @CacheEvict(cacheNames = {"item", "itemList", "cart"}, allEntries = true)
    public Mono<ItemDto> changeItemCountFromItemPage(Long itemId, Action action, Long userId) {
        Mono<ItemDto> itemDtoMono = changeItemCount(itemId, action, userId)
                .flatMap(item -> itemRepository.findByIdWithCount(userId, itemId))
                .map(itemMapper::toItemDto);
        log.info("Count of Item with ID {} has been changed from item page.", itemId);
        return itemDtoMono;
    }

    @Override
    public Mono<CartPageResponse> getItemCartList(Long userId) {
        Mono<CartPageResponse> cartPageResponseMono = cartCacheService.getCart(userId)
                .flatMap(cart -> resolvePaymentAvailability(cart.total(), userId)
                        .map(payment -> new CartPageResponse(cart, payment)));
        log.info("Cart page has been returned for user {}", userId);
        return cartPageResponseMono;
    }

    @Override
    @CacheEvict(cacheNames = {"item", "itemList", "cart"}, allEntries = true)
    public Mono<CartPageResponse> changeItemCountFromCartPage(Long itemId, Action action, Long userId) {
        Mono<CartPageResponse> cartPageResponseMono =
                changeItemCount(itemId, action, userId).then(getItemCartList(userId));
        log.info("Count of Item with ID {} has been changed from cart page for user {}.", itemId, userId);
        return cartPageResponseMono;
    }

    private Flux<ItemWithCount> resolveItemsFlux(Sort sort, boolean hasSearch, String search,
                                                 Long userId, int limit, int offset) {
        if (hasSearch) {
            return switch (sort) {
                case ALPHA -> itemRepository.searchOrderByTitle(userId, search, limit, offset);
                case PRICE -> itemRepository.searchOrderByPrice(userId, search, limit, offset);
                default    -> itemRepository.searchOrderById(userId, search, limit, offset);
            };
        } else {
            return switch (sort) {
                case ALPHA -> itemRepository.findAllOrderByTitle(userId, limit, offset);
                case PRICE -> itemRepository.findAllOrderByPrice(userId, limit, offset);
                default    -> itemRepository.findAllOrderById(userId, limit, offset);
            };
        }
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

    private Mono<Item> changeItemCount(Long itemId, Action action, Long userId) {
        return findItemById(itemId).flatMap(item ->
                cartItemRepository.findCount(userId, itemId).flatMap(currentCount -> {
                    if (action == Action.MINUS && currentCount == 0) {
                        return Mono.error(new BadRequestException("Count cannot be less than 0"));
                    }

                    if (action == Action.DELETE) {
                        return cartItemRepository.delete(userId, itemId).thenReturn(item);
                    }

                    int newCount = action == Action.PLUS ? currentCount + 1 : currentCount - 1;
                    return cartItemRepository.upsert(userId, itemId, newCount).thenReturn(item);
                })
        );
    }

    private Mono<Item> findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(String.format("Item with ID = %d not found", itemId))));
    }

    private Mono<PaymentStatusDto> resolvePaymentAvailability(long total, Long userId) {
        return paymentClient.getBalance(userId)
                .map(balance -> {
                    if (balance.getBalance() == null || balance.getBalance() < total) {
                        return new PaymentStatusDto(false,
                                "Недостаточно средств на счёте для оформления заказа.");
                    }
                    return new PaymentStatusDto(true, null);
                })
                .onErrorResume(e -> {
                    log.warn("Payment service is unavailable: {}", e.getMessage());
                    return Mono.just(new PaymentStatusDto(false,
                            "Сервис платежей недоступен. Оформление заказа временно невозможно."));
                });
    }
}