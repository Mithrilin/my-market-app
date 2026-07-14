package ru.yandex.practicum.my_market_app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;
import ru.yandex.practicum.my_market_app.dto.ItemCartListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.enums.Sort;
import ru.yandex.practicum.my_market_app.exception.BadRequestException;
import ru.yandex.practicum.my_market_app.exception.NotFoundException;
import ru.yandex.practicum.my_market_app.mapper.ItemMapper;
import ru.yandex.practicum.my_market_app.model.Item;
import ru.yandex.practicum.my_market_app.repository.ItemRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemMapper.toItemDto(testItem)).thenReturn(testItemDto);

        ItemDto result = itemService.getItemById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Test Item");
        verify(itemRepository).findById(1L);
        verify(itemMapper).toItemDto(testItem);
    }

    @Test
    @DisplayName("Getting Item by ID when Item not found")
    void getPostById_whenPostNotFound_shouldThrowNotFoundException() {
        when(itemRepository.findById(111L))
                .thenThrow(new NotFoundException(String.format("The Item with ID = %s not found", 111L)));

        assertThatThrownBy(() -> itemService.getItemById(111L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(String.format("The Item with ID = %s not found", 111L));
    }

    @Test
    @DisplayName("Successful getting Item List with pagination")
    void getItemList_shouldReturnItemListWithPagination() {
        List<Item> items = TestDataFactory.createTestItemList(3);
        Page<Item> page = new PageImpl<>(items, PageRequest.of(0, 5), 3);
        List<ItemDto> itemDtoList = Arrays.asList(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 1),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 2),
                TestDataFactory.createTestItemDto(3L, "Item 3", 300L, 3)
        );

        ItemListSearchRequest request = TestDataFactory.createDefaultSearchRequest();

        when(itemRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(itemMapper.toItemDtoList(items)).thenReturn(itemDtoList);

        ItemListResponse response = itemService.getItemList(request);

        assertThat(response).isNotNull();
        assertThat(response.paging()).isNotNull();
        assertThat(response.splittedItemDtoList()).hasSize(1);
        assertThat(response.splittedItemDtoList().getFirst()).hasSize(3);
        verify(itemRepository).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Successful getting Item List with search")
    void getItemList_shouldReturnItemListWithSearch() {
        String searchTerm = "Test";
        List<Item> items = Collections.singletonList(testItem);
        Page<Item> page = new PageImpl<>(items, PageRequest.of(0, 5), 1);
        List<ItemDto> itemDtoList = Collections.singletonList(testItemDto);

        ItemListSearchRequest request = TestDataFactory.createSearchRequest(searchTerm, Sort.NO, 1, 5);

        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                eq(searchTerm), eq(searchTerm), any(PageRequest.class)))
                .thenReturn(page);
        when(itemMapper.toItemDtoList(items)).thenReturn(itemDtoList);

        ItemListResponse response = itemService.getItemList(request);

        assertThat(response).isNotNull();
        assertThat(response.splittedItemDtoList()).isNotEmpty();
        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                eq(searchTerm), eq(searchTerm), any(PageRequest.class));
    }

    @Test
    @DisplayName("Successful increase item count")
    void changeItemCountFromItemPage_shouldIncreaseItemCount() {
        Item itemWithNewCount = TestDataFactory.createTestItem(1L, "Test Item", "Description",
                100L, 6);
        ItemDto expectedDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 6);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(itemWithNewCount);
        when(itemMapper.toItemDto(itemWithNewCount)).thenReturn(expectedDto);

        ItemDto result = itemService.changeItemCountFromItemPage(1L, Action.PLUS);

        assertThat(result).isNotNull();
        assertThat(result.count()).isEqualTo(6);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    @DisplayName("Successful decrease item count")
    void changeItemCountFromItemPage_shouldDecreaseItemCount() {
        Item itemWithNewCount = TestDataFactory.createTestItem(1L, "Test Item", "Description",
                100L, 4);
        ItemDto expectedDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 4);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(itemWithNewCount);
        when(itemMapper.toItemDto(itemWithNewCount)).thenReturn(expectedDto);

        ItemDto result = itemService.changeItemCountFromItemPage(1L, Action.MINUS);

        assertThat(result).isNotNull();
        assertThat(result.count()).isEqualTo(4);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    @DisplayName("Decrease item when count below 0")
    void changeItemCountFromItemPage_shouldThrowBadRequestExceptionWhenCountBelowZero() {
        Item zeroCountItem = TestDataFactory.createTestItem(1L, "Test Item", "Description",
                100L, 0);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(zeroCountItem));

        assertThatThrownBy(() -> itemService.changeItemCountFromItemPage(1L, Action.MINUS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Count cannot be less than 0");
    }

    @Test
    @DisplayName("Change item count when action equal DELETE")
    void changeItemCountFromItemPage_shouldThrowBadRequestExceptionWhenDeleteActionOnItemPage() {
        assertThatThrownBy(() -> itemService.changeItemCountFromItemPage(1L, Action.DELETE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Action cannot be equal DELETE");
    }

    @Test
    @DisplayName("Successful getting cart items")
    void getItemCartList_shouldReturnCartItems() {
        List<Item> itemsWithCount = Arrays.asList(
                TestDataFactory.createTestItem(1L, "Item 1", "Desc 1", 100L, 2),
                TestDataFactory.createTestItem(2L, "Item 2", "Desc 2", 200L, 3)
        );
        List<ItemDto> itemDtoList = Arrays.asList(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 2),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 3)
        );

        when(itemRepository.findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(itemsWithCount);
        when(itemMapper.toItemDtoList(itemsWithCount)).thenReturn(itemDtoList);

        ItemCartListResponse response = itemService.getItemCartList();

        assertThat(response).isNotNull();
        assertThat(response.itemDtoList()).hasSize(2);
        assertThat(response.total()).isEqualTo(300L);
        verify(itemRepository).findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART);
    }

    @Test
    @DisplayName("Successful change item count in cart")
    void changeItemCountFromCartPage_shouldChangeItemCountInCart() {
        Item updatedItem = TestDataFactory.createTestItem(1L, "Test Item", "Description",
                100L, 6);
        List<Item> cartItems = Collections.singletonList(updatedItem);
        List<ItemDto> itemDtoList = Collections.singletonList(TestDataFactory.createTestItemDto(1L, "Test Item",
                100L, 6));

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(updatedItem);
        when(itemRepository.findByCountGreaterThanEqual(ItemServiceImpl.MIN_COUNT_ITEMS_IN_CART))
                .thenReturn(cartItems);
        when(itemMapper.toItemDtoList(cartItems)).thenReturn(itemDtoList);

        ItemCartListResponse response = itemService.changeItemCountFromCartPage(1L, Action.PLUS);

        assertThat(response).isNotNull();
        assertThat(response.itemDtoList()).hasSize(1);
        assertThat(response.itemDtoList().getFirst().count()).isEqualTo(6);
    }
}