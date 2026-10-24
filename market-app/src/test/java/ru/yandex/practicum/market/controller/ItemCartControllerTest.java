package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.CartDto;
import ru.yandex.practicum.market.dto.CartPageResponse;
import ru.yandex.practicum.market.dto.ItemDto;
import ru.yandex.practicum.market.dto.PaymentStatusDto;
import ru.yandex.practicum.market.enums.Action;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.util.TestDataFactory;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(ItemCartController.class)
class ItemCartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    private CartPageResponse testCartResponse;

    @BeforeEach
    void setUp() {
        List<ItemDto> items = List.of(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 2),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 3)
        );
        testCartResponse = new CartPageResponse(
                new CartDto(items, 700L),
                new PaymentStatusDto(true, null));
    }

    @Test
    @DisplayName("Successful getting Cart Page")
    void getItemCartList_shouldReturnCartPage() {
        when(itemService.getItemCartList()).thenReturn(Mono.just(testCartResponse));

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful change of the quantity of goods in the Cart (PLUS)")
    void changeItemCountFromCartPage_shouldIncreaseItemCountInCart() {
        when(itemService.changeItemCountFromCartPage(1L, Action.PLUS))
                .thenReturn(Mono.just(testCartResponse));

        webTestClient.post()
                .uri("/cart/items?id=1&action=PLUS")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful decrease count in Cart")
    void changeItemCountFromCartPage_shouldDecreaseItemCountInCart() {
        when(itemService.changeItemCountFromCartPage(1L, Action.MINUS))
                .thenReturn(Mono.just(testCartResponse));

        webTestClient.post()
                .uri("/cart/items?id=1&action=MINUS")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Return Error when negative Id")
    void shouldReturnErrorForNegativeId() {
        webTestClient.post()
                .uri("/cart/items?id=-1&action=PLUS")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Return Error when missing Id")
    void shouldReturnErrorForMissingId() {
        webTestClient.post()
                .uri("/cart/items?action=PLUS")
                .exchange()
                .expectStatus().isBadRequest();
    }
}