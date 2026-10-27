package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.config.SecurityConfig;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.ItemListResponse;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.dto.Paging;
import ru.yandex.practicum.market.enums.Action;
import ru.yandex.practicum.market.model.CustomUserDetails;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.util.TestDataFactory;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private ReactiveUserDetailsService reactiveUserDetailsService;

    private ItemDto testItemDto;
    private ItemListResponse testItemListResponse;
    private CustomUserDetails testUser;

    @BeforeEach
    void setUp() {
        testItemDto = TestDataFactory.createTestItemDto(1L, "Test Item", 100L, 5);

        Paging paging = new Paging(5, 1, false, true);
        List<List<ItemDto>> items = Collections.singletonList(Collections.singletonList(testItemDto));
        testItemListResponse = new ItemListResponse(items, paging);
        testUser = new CustomUserDetails(TestDataFactory.createTestUser(1L, "user", "password", "ROLE_USER"));
    }

    @Test
    @DisplayName("Successful getting Item Page by ID")
    void getItemById_shouldReturnItemPageById() {
        when(itemService.getItemById(eq(1L), anyLong())).thenReturn(Mono.just(testItemDto));

        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful getting Item List")
    void getItemList_shouldReturnItemList() {
        when(itemService.getItemList(any(ItemListSearchRequest.class), anyLong()))
                .thenReturn(Mono.just(testItemListResponse));

        webTestClient.get().uri("/items?pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful getting Item List with search and sort")
    void getItemList_shouldReturnItemListWithSearchAndSort() {
        when(itemService.getItemList(any(ItemListSearchRequest.class), anyLong()))
                .thenReturn(Mono.just(testItemListResponse));

        webTestClient.get().uri("/items?search=test&sort=ALPHA&pageNumber=1&pageSize=10")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful change item count on Item Page")
    void changeItemCountFromItemPage_shouldChangeItemCountOnItemPage() {
        when(itemService.changeItemCountFromItemPage(eq(1L), eq(Action.PLUS), anyLong()))
                .thenReturn(Mono.just(testItemDto));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockUser(testUser))
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/items/1?action=PLUS")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful change of the product quantity on the list page (redirect)")
    void shouldChangeItemCountOnItemListPage() {
        when(itemService.changeItemCountFromItemPage(eq(1L), eq(Action.PLUS), anyLong()))
                .thenReturn(Mono.just(testItemDto));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockUser(testUser))
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/items")
                .bodyValue("id=1&action=PLUS&pageNumber=1&pageSize=5")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*/items.*");
    }

    @Test
    @DisplayName("Successful saving of search parameters during redirect")
    void shouldPreserveSearchParamsOnRedirect() {
        when(itemService.changeItemCountFromItemPage(eq(1L), eq(Action.MINUS), anyLong()))
                .thenReturn(Mono.just(testItemDto));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockUser(testUser))
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/items?id=1&action=MINUS&search=test&sort=PRICE&pageNumber=2&pageSize=10")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*search=test.*")
                .expectHeader().valueMatches("Location", ".*sort=PRICE.*");
    }

    @Test
    @DisplayName("Anonymous cannot change item count")
    void anonymousUser_cannotChangeItemCount() {
        webTestClient.post().uri("/items/1?action=PLUS")
                .exchange()
                .expectStatus().isForbidden();
    }
}