package ru.yandex.practicum.my_market_app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.dto.Paging;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.service.ItemService;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    private ItemDto testItemDto;
    private ItemListResponse testItemListResponse;

    @BeforeEach
    void setUp() {
        testItemDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 5);

        Paging paging = new Paging(5, 1, false, true);
        List<List<ItemDto>> items = Collections.singletonList(Collections.singletonList(testItemDto));
        testItemListResponse = new ItemListResponse(items, paging);
    }

    @Test
    @DisplayName("Successful getting Item Page by ID")
    void getItemById_shouldReturnItemPageById() {
        when(itemService.getItemById(1L)).thenReturn(Mono.just(testItemDto));

        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful getting Item List")
    void getItemList_shouldReturnItemList() {
        when(itemService.getItemList(any(ItemListSearchRequest.class)))
                .thenReturn(Mono.just(testItemListResponse));

        webTestClient.get().uri("/items?pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful getting Item List with search and sort")
    void getItemList_shouldReturnItemListWithSearchAndSort() {
        when(itemService.getItemList(any(ItemListSearchRequest.class)))
                .thenReturn(Mono.just(testItemListResponse));

        webTestClient.get().uri("/items?search=test&sort=ALPHA&pageNumber=1&pageSize=10")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful change item count on Item Page")
    void changeItemCountFromItemPage_shouldChangeItemCountOnItemPage() {
        when(itemService.changeItemCountFromItemPage(1L, Action.PLUS))
                .thenReturn(Mono.just(testItemDto));

        webTestClient.post()
                .uri("/items/1?action=PLUS")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful change of the product quantity on the list page (redirect)")
    void shouldChangeItemCountOnItemListPage() {
        when(itemService.changeItemCountFromItemPage(1L, Action.PLUS))
                .thenReturn(Mono.just(testItemDto));

        webTestClient.post().uri("/items")
                .bodyValue("id=1&action=PLUS&pageNumber=1&pageSize=5")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*/items.*");
    }

    @Test
    @DisplayName("Successful saving of search parameters during redirect")
    void shouldPreserveSearchParamsOnRedirect() {
        when(itemService.changeItemCountFromItemPage(1L, Action.MINUS))
                .thenReturn(Mono.just(testItemDto));

        webTestClient.post()
                .uri("/items?id=1&action=MINUS&search=test&sort=PRICE&pageNumber=2&pageSize=10")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*search=test.*")
                .expectHeader().valueMatches("Location", ".*sort=PRICE.*");
    }
}