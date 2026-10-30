package ru.yandex.practicum.market.service;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.CartDto;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.mapper.ItemMapper;
import ru.yandex.practicum.market.repository.ItemRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class CartCacheService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Cacheable(cacheNames = "cart", key = "#userId")
    public Mono<CartDto> getCart(Long userId) {
        return itemRepository.findCartItemsByUserId(userId, ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART)
                .collectList()
                .map(items -> {
                    long total = items.stream()
                            .mapToLong(i -> i.getPrice() * i.getCount())
                            .sum();
                    List<ItemDto> dtos = itemMapper.toItemDtoList(items);
                    return new CartDto(dtos, total);
                });
    }
}