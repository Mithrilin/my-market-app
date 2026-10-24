package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.OrderDto;
import ru.yandex.practicum.market.service.OrderService;

import java.util.Collections;

import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testOrderDto = OrderDto.builder()
                .id(1L)
                .items(Collections.emptyList())
                .totalSum(1000L)
                .build();
    }

    @Test
    @DisplayName("Successful getting Order List")
    void getOrderList_shouldReturnOrderList() {
        when(orderService.getOrderList()).thenReturn(Flux.just(testOrderDto));

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful getting Order by ID")
    void getOrderById_shouldReturnOrderPageById() {
        when(orderService.getOrderById(1L)).thenReturn(Mono.just(testOrderDto));

        webTestClient.get().uri("/orders/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful return of the new order page with flag newOrder=true")
    void getOrderById_shouldReturnNewOrderPage() {
        when(orderService.getOrderById(1L)).thenReturn(Mono.just(testOrderDto));

        webTestClient.get().uri("/orders/1?newOrder=true")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Successful Order creation")
    void createOrder_shouldCreateOrder() {
        when(orderService.createOrder()).thenReturn(Mono.just(testOrderDto));

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/orders/1?newOrder=true");
    }

    @Test
    @DisplayName("Order creation when negative order Id")
    void shouldReturnErrorForNegativeOrderId() {
        webTestClient.get().uri("/orders/-1")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Order creation when order Id is 0")
    void shouldReturnErrorForZeroOrderId() {
        webTestClient.get().uri("/orders/0")
                .exchange()
                .expectStatus().isBadRequest();
    }
}