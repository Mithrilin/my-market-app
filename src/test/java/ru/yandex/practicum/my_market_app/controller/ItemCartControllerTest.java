package ru.yandex.practicum.my_market_app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.my_market_app.dto.ItemCartListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.service.ItemService;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ItemCartController.class)
class ItemCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    private ItemCartListResponse testCartResponse;

    @BeforeEach
    void setUp() {
        List<ItemDto> items = List.of(
                TestDataFactory.createTestItemDto(1L, "Item 1", 100L, 2),
                TestDataFactory.createTestItemDto(2L, "Item 2", 200L, 3)
        );
        testCartResponse = new ItemCartListResponse(items, 700L);
    }

    @Test
    void getItemCartList() {
    }
    @Test
    @DisplayName("Successful getting Cart Page")
    void getItemCartList_shouldReturnCartPage() throws Exception {
        when(itemService.getItemCartList()).thenReturn(testCartResponse);

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items", "total"))
                .andExpect(model().attribute("total", 700L));
    }

    @Test
    @DisplayName("Successful change of the quantity of goods in the Cart")
    void changeItemCountFromCartPage_shouldChangeItemCountInCart() throws Exception {
        when(itemService.changeItemCountFromCartPage(1L, Action.PLUS)).thenReturn(testCartResponse);

        mockMvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items", "total"));
    }

    @Test
    @DisplayName("Successful decrease count in Cart")
    void changeItemCountFromCartPage_shouldDecreaseItemCountInCart() throws Exception {
        when(itemService.changeItemCountFromCartPage(1L, Action.MINUS)).thenReturn(testCartResponse);

        mockMvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", "MINUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    @DisplayName("Return Error when negative Id")
    void shouldReturnErrorForNegativeId() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("id", "-1")
                        .param("action", "PLUS"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Return Error when missing Id")
    void shouldReturnErrorForMissingId() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("action", "PLUS"))
                .andExpect(status().is5xxServerError());
    }
}