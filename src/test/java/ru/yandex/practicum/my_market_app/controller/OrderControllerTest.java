package ru.yandex.practicum.my_market_app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.my_market_app.dto.OrderDto;
import ru.yandex.practicum.my_market_app.service.OrderService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void getOrderList_shouldReturnOrderList() throws Exception {
        List<OrderDto> orders = Collections.singletonList(testOrderDto);
        when(orderService.getOrderList()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", orders));
    }

    @Test
    void getOrderById() {
    }
    @Test
    @DisplayName("Successful getting Order by ID")
    void getOrderById_shouldReturnOrderPageById() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(testOrderDto);

        mockMvc.perform(get("/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order", "newOrder"))
                .andExpect(model().attribute("newOrder", false));
    }

    @Test
    @DisplayName("Successful return of the new order page with flag newOrder=true")
    void getOrderById_shouldReturnNewOrderPage() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(testOrderDto);

        mockMvc.perform(get("/orders/{id}", 1L)
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", true));
    }

    @Test
    @DisplayName("Successful Order creation")
    void createOrder_shouldCreateOrder() throws Exception {
        when(orderService.createOrder()).thenReturn(testOrderDto);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1?newOrder=true"));
    }

    @Test
    @DisplayName("Order creation when negative order Id")
    void shouldReturnErrorForNegativeOrderId() throws Exception {
        mockMvc.perform(get("/orders/{id}", -1L))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Order creation when order Id is 0")
    void shouldReturnErrorForZeroOrderId() throws Exception {
        mockMvc.perform(get("/orders/{id}", 0L))
                .andExpect(status().is5xxServerError());
    }
}