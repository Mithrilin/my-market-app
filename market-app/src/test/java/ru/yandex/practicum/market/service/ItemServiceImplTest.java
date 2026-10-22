package ru.yandex.practicum.market.service;

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
import ru.yandex.practicum.market.util.TestDataFactory;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.enums.Action;
import ru.yandex.practicum.market.enums.Sort;
import ru.yandex.practicum.market.exception.BadRequestException;
import ru.yandex.practicum.market.exception.NotFoundException;
import ru.yandex.practicum.market.mapper.ItemMapper;
import ru.yandex.practicum.market.model.Item;
import ru.yandex.practicum.market.repository.ItemRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

        verify(itemRepository).findById(1L);
        verify(itemMapper).toItemDto(testItem);
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
    @DisplayName("getItemList without search uses findAllOrderById for Sort.NO")
    void getItemList_noSearch_sortNo_shouldUseFindAllOrderById() {
        List<Item> items = TestDataFactory.createTestItemList(3);
        List<ItemDto> dtoList = List.of(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 1),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 2),
                TestDataFactory.createTestItemDto(3L, "Item 3", 300L, 3));

        ItemListSearchRequest req = TestDataFactory.createDefaultSearchRequest(); // sort=NO, page=1, size=5

        when(itemRepository.findAllOrderById(5, 0)).thenReturn(Flux.fromIterable(items));
        when(itemRepository.count()).thenReturn(Mono.just(3L));
        when(itemMapper.toItemDtoList(items)).thenReturn(dtoList);

        StepVerifier.create(itemService.getItemList(req))
                .assertNext(response -> {
                    assertThat(response.paging().pageNumber()).isEqualTo(1);
                    assertThat(response.paging().pageSize()).isEqualTo(5);
                    assertThat(response.paging().hasPrevious()).isFalse();
                    assertThat(response.paging().hasNext()).isFalse();
                    assertThat(response.splittedItemDtoList()).hasSize(1);
                })
                .verifyComplete();

        verify(itemRepository).findAllOrderById(5, 0);
        verify(itemRepository).count();
        verify(itemRepository, never()).findAllOrderByTitle(anyInt(), anyInt());
        verify(itemRepository, never()).findAllOrderByPrice(anyInt(), anyInt());
    }

    @Test
    @DisplayName("getItemList without search uses findAllOrderByTitle for Sort.ALPHA")
    void getItemList_noSearch_sortAlpha_shouldUseFindAllOrderByTitle() {
        ItemListSearchRequest req = TestDataFactory.createSearchRequest(null, Sort.ALPHA, 1, 5);

        when(itemRepository.findAllOrderByTitle(5, 0)).thenReturn(Flux.empty());
        when(itemRepository.count()).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.getItemList(req))
                .assertNext(response -> {
                    assertThat(response.splittedItemDtoList()).isEmpty();
                    assertThat(response.paging().hasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).findAllOrderByTitle(5, 0);
    }

    @Test
    @DisplayName("getItemList without search uses findAllOrderByPrice for Sort.PRICE")
    void getItemList_noSearch_sortPrice_shouldUseFindAllOrderByPrice() {
        ItemListSearchRequest req = TestDataFactory.createSearchRequest(null, Sort.PRICE, 1, 5);

        when(itemRepository.findAllOrderByPrice(5, 0)).thenReturn(Flux.empty());
        when(itemRepository.count()).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.getItemList(req))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).findAllOrderByPrice(5, 0);
    }

    @Test
    @DisplayName("getItemList with search uses searchOrderById for Sort.NO")
    void getItemList_withSearch_sortNo_shouldUseSearchOrderById() {
        String searchTerm = "Test";
        List<Item> items = List.of(testItem);
        List<ItemDto> dtoList = List.of(testItemDto);

        ItemListSearchRequest req = TestDataFactory.createSearchRequest(searchTerm, Sort.NO, 1, 5);

        when(itemRepository.searchOrderById(eq(searchTerm), eq(5), eq(0)))
                .thenReturn(Flux.fromIterable(items));
        when(itemRepository.countSearch(searchTerm)).thenReturn(Mono.just(1L));
        when(itemMapper.toItemDtoList(items)).thenReturn(dtoList);

        StepVerifier.create(itemService.getItemList(req))
                .assertNext(response -> assertThat(response.splittedItemDtoList()).isNotEmpty())
                .verifyComplete();

        verify(itemRepository).searchOrderById(searchTerm, 5, 0);
        verify(itemRepository).countSearch(searchTerm);
    }

    @Test
    @DisplayName("getItemList with search uses searchOrderByTitle for Sort.ALPHA")
    void getItemList_withSearch_sortAlpha_shouldUseSearchOrderByTitle() {
        String searchTerm = "Test";
        ItemListSearchRequest req = TestDataFactory.createSearchRequest(searchTerm, Sort.ALPHA, 1, 5);

        when(itemRepository.searchOrderByTitle(eq(searchTerm), eq(5), eq(0)))
                .thenReturn(Flux.empty());
        when(itemRepository.countSearch(searchTerm)).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.getItemList(req))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).searchOrderByTitle(searchTerm, 5, 0);
    }

    @Test
    @DisplayName("getItemList with search uses searchOrderByPrice for Sort.PRICE")
    void getItemList_withSearch_sortPrice_shouldUseSearchOrderByPrice() {
        String searchTerm = "Test";
        ItemListSearchRequest req = TestDataFactory.createSearchRequest(searchTerm, Sort.PRICE, 1, 5);

        when(itemRepository.searchOrderByPrice(eq(searchTerm), eq(5), eq(0)))
                .thenReturn(Flux.empty());
        when(itemRepository.countSearch(searchTerm)).thenReturn(Mono.just(0L));

        StepVerifier.create(itemService.getItemList(req))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).searchOrderByPrice(searchTerm, 5, 0);
    }

    @Test
    @DisplayName("getItemList second page passes correct offset")
    void getItemList_shouldPassCorrectOffsetForSecondPage() {
        ItemListSearchRequest req = TestDataFactory.createSearchRequest(null, Sort.NO, 2, 5);

        when(itemRepository.findAllOrderById(5, 5)).thenReturn(Flux.empty());
        when(itemRepository.count()).thenReturn(Mono.just(12L));

        StepVerifier.create(itemService.getItemList(req))
                .assertNext(response -> {
                    assertThat(response.paging().hasPrevious()).isTrue();
                    assertThat(response.paging().hasNext()).isTrue();
                })
                .verifyComplete();

        verify(itemRepository).findAllOrderById(5, 5);
    }

    @Test
    @DisplayName("getItemList last page has hasNext=false")
    void getItemList_lastPage_shouldNotHaveNext() {
        ItemListSearchRequest req = TestDataFactory.createSearchRequest(null, Sort.NO, 3, 5);

        when(itemRepository.findAllOrderById(5, 10)).thenReturn(Flux.empty());
        when(itemRepository.count()).thenReturn(Mono.just(12L));

        StepVerifier.create(itemService.getItemList(req))
                .assertNext(response -> {
                    assertThat(response.paging().hasPrevious()).isTrue();
                    assertThat(response.paging().hasNext()).isFalse();
                })
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

        verify(itemRepository).save(any(Item.class));
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

        verify(itemRepository).save(any(Item.class));
    }

    @Test
    @DisplayName("Decrease item when count is 0 — BadRequestException")
    void changeItemCountFromItemPage_shouldThrowBadRequestExceptionWhenCountBelowZero() {
        Item zeroCountItem = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 0);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(zeroCountItem));

        StepVerifier.create(itemService.changeItemCountFromItemPage(1L, Action.MINUS))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Count cannot be less than 0");
                })
                .verify();

        verifyNoInteractions(itemMapper);
    }

    @Test
    @DisplayName("DELETE action on item page — BadRequestException")
    void changeItemCountFromItemPage_shouldThrowBadRequestExceptionWhenDeleteActionOnItemPage() {
        StepVerifier.create(itemService.changeItemCountFromItemPage(1L, Action.DELETE))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(BadRequestException.class);
                    assertThat(e.getMessage()).contains("Action cannot be equal DELETE");
                })
                .verify();

        verifyNoInteractions(itemRepository);
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

        verify(itemRepository).findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART);
    }

    @Test
    @DisplayName("Cart returns empty list when no items")
    void getItemCartList_shouldReturnEmptyCart() {
        when(itemRepository.findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(Flux.empty());
        when(itemMapper.toItemDtoList(List.of())).thenReturn(List.of());

        StepVerifier.create(itemService.getItemCartList())
                .assertNext(r -> {
                    assertThat(r.itemDtoList()).isEmpty();
                    assertThat(r.total()).isZero();
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

    @Test
    @DisplayName("DELETE action from cart sets count to 0 and removes from cart")
    void changeItemCountFromCartPage_shouldRemoveItemOnDelete() {
        Item deletedItem = TestDataFactory.createTestItem(1L, "Test Item", "Description", 100L, 0);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(deletedItem));
        when(itemRepository.findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(Flux.empty());
        when(itemMapper.toItemDtoList(List.of())).thenReturn(List.of());

        StepVerifier.create(itemService.changeItemCountFromCartPage(1L, Action.DELETE))
                .assertNext(r -> {
                    assertThat(r.itemDtoList()).isEmpty();
                    assertThat(r.total()).isZero();
                })
                .verifyComplete();

        verify(itemRepository).save(any(Item.class));
    }
}