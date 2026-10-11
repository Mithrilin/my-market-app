package ru.yandex.practicum.my_market_app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.enums.Sort;
import ru.yandex.practicum.my_market_app.exception.BadRequestException;
import ru.yandex.practicum.my_market_app.exception.NotFoundException;
import ru.yandex.practicum.my_market_app.mapper.ItemMapper;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.repository.ItemRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item testItem;
    private ItemDto testItemDto;

    @BeforeEach
    void setUp() {
        testItem = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 5);
        testItemDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 5);
    }

    @Test
    @DisplayName("Successful getting Item")
    void getItemById_whenItemExists_shouldReturnItemDto() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemMapper.toItemDto(testItem)).thenReturn(testItemDto);

        StepVerifier.create(itemService.getItemById(1L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.title()).isEqualTo("Test Item");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Getting Item by ID when Item not found")
    void getItemById_whenItemNotFound_shouldThrowNotFoundException() {
        when(itemRepository.findById(111L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItemById(111L))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(NotFoundException.class);
                    assertThat(e.getMessage()).contains("Item with ID = 111 not found");
                })
                .verify();
    }

    @Test
    @DisplayName("Successful getting Item List with search")
    void getItemList_shouldReturnItemListWithSearch() {
        String searchTerm = "Test";
        List<Item> items = List.of(testItem);
        List<ItemDto> dtoList = List.of(testItemDto);

        ItemListSearchRequest req = TestDataFactory.createSearchRequest(searchTerm, Sort.NO, 1, 5);

        when(itemRepository.search(eq(searchTerm), anyInt(), anyInt()))
                .thenReturn(Flux.fromIterable(items));
        when(itemRepository.countSearch(searchTerm)).thenReturn(Mono.just(1L));
        when(itemMapper.toItemDtoList(items)).thenReturn(dtoList);

        StepVerifier.create(itemService.getItemList(req))
                .assertNext(response -> assertThat(response.splittedItemDtoList()).isNotEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Successful increase item count")
    void changeItemCountFromItemPage_shouldIncreaseItemCount() {
        Item itemWithNewCount = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 6);
        ItemDto expectedDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 6);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(itemWithNewCount));
        when(itemMapper.toItemDto(itemWithNewCount)).thenReturn(expectedDto);

        StepVerifier.create(itemService.changeItemCountFromItemPage(1L, Action.PLUS))
                .assertNext(dto -> assertThat(dto.count()).isEqualTo(6))
                .verifyComplete();
    }

    @Test
    @DisplayName("Successful decrease item count")
    void changeItemCountFromItemPage_shouldDecreaseItemCount() {
        Item itemWithNewCount = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 4);
        ItemDto expectedDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 4);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(itemWithNewCount));
        when(itemMapper.toItemDto(itemWithNewCount)).thenReturn(expectedDto);

        StepVerifier.create(itemService.changeItemCountFromItemPage(1L, Action.MINUS))
                .assertNext(dto -> assertThat(dto.count()).isEqualTo(4))
                .verifyComplete();
    }

    @Test
    @DisplayName("Decrease item when count below 0")
    void changeItemCountFromItemPage_shouldThrowBadRequestExceptionWhenCountBelowZero() {
        Item zeroCountItem = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 0);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(zeroCountItem));

        StepVerifier.create(itemService.changeItemCountFromItemPage(1L, Action.MINUS))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Count cannot be less than 0");
                })
                .verify();
    }

    @Test
    @DisplayName("Change item count when action equal DELETE on Item page")
    void changeItemCountFromItemPage_shouldThrowBadRequestExceptionWhenDeleteActionOnItemPage() {
        StepVerifier.create(itemService.changeItemCountFromItemPage(1L, Action.DELETE))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Action cannot be equal DELETE");
                })
                .verify();
    }

    @Test
    @DisplayName("Successful getting cart items")
    void getItemCartList_shouldReturnCartItems() {
        List<Item> itemsWithCount = List.of(
                TestDataFactory.createTestItem(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItem(2L, "Item 2", "Desc 2", 200L, 3));
        List<ItemDto> dtoList = List.of(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 2),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 3));

        when(itemRepository.findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(Flux.fromIterable(itemsWithCount));
        when(itemMapper.toItemDtoList(itemsWithCount)).thenReturn(dtoList);

        StepVerifier.create(itemService.getItemCartList())
                .assertNext(r -> {
                    assertThat(r.itemDtoList()).hasSize(2);
                    assertThat(r.total()).isEqualTo(800L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Successful change item count in cart")
    void changeItemCountFromCartPage_shouldChangeItemCountInCart() {
        Item updatedItem = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 6);
        ItemDto updatedDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 6);
        List<Item> cartItems = List.of(updatedItem);
        List<ItemDto> dtoList = List.of(updatedDto);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(updatedItem));
        when(itemRepository.findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(Flux.fromIterable(cartItems));
        when(itemMapper.toItemDtoList(cartItems)).thenReturn(dtoList);

        StepVerifier.create(itemService.changeItemCountFromCartPage(1L, Action.PLUS))
                .assertNext(r -> {
                    assertThat(r.itemDtoList()).hasSize(1);
                    assertThat(r.itemDtoList().getFirst().count()).isEqualTo(6);
                })
                .verifyComplete();
    }
}