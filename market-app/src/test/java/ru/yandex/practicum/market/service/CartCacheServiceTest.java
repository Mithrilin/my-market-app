package ru.yandex.practicum.market.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.mapper.ItemMapper;
import ru.yandex.practicum.market.model.ItemWithCount;
import ru.yandex.practicum.market.repository.ItemRepository;
import ru.yandex.practicum.market.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartCacheServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private CartCacheService cartCacheService;

    private List<ItemWithCount> itemsWithCount;
    private List<ItemDto> dtoList;

    @BeforeEach
    void setUp() {
        itemsWithCount = List.of(
                TestDataFactory.createTestItemWithCount(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItemWithCount(2L, "Item 2", "Desc 2", 200L, 3));
        dtoList = List.of(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 2),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 3));
    }

    @Test
    @DisplayName("getCart builds CartDto with items and total")
    void getCart_shouldBuildCartDto() {
        when(itemRepository.findCartItemsByUserId(USER_ID, ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(Flux.fromIterable(itemsWithCount));
        when(itemMapper.toItemDtoList(itemsWithCount)).thenReturn(dtoList);

        StepVerifier.create(cartCacheService.getCart(USER_ID))
                .assertNext(cart -> {
                    assertThat(cart.itemDtoList()).hasSize(2);
                    assertThat(cart.total()).isEqualTo(800L);
                })
                .verifyComplete();

        verify(itemRepository).findCartItemsByUserId(USER_ID, ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART);
        verify(itemMapper).toItemDtoList(itemsWithCount);
    }

    @Test
    @DisplayName("getCart returns empty cart when no items")
    void getCart_shouldReturnEmptyCart() {
        when(itemRepository.findCartItemsByUserId(USER_ID, ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(Flux.empty());
        when(itemMapper.toItemDtoList(List.of())).thenReturn(List.of());

        StepVerifier.create(cartCacheService.getCart(USER_ID))
                .assertNext(cart -> {
                    assertThat(cart.itemDtoList()).isEmpty();
                    assertThat(cart.total()).isZero();
                })
                .verifyComplete();
    }
}