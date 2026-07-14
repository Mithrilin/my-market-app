package ru.yandex.practicum.my_market_app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.dto.Paging;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.service.ItemService;
import ru.yandex.practicum.my_market_app.util.TestDataFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getItemById_shouldReturnItemPageById() throws Exception {
        when(itemService.getItemById(1L)).thenReturn(testItemDto);

        mockMvc.perform(get("/items/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", testItemDto));
    }

    @Test
    @DisplayName("Successful getting Item List")
    void getItemList_shouldReturnItemList() throws Exception {
        when(itemService.getItemList(any(ItemListSearchRequest.class))).thenReturn(testItemListResponse);

        mockMvc.perform(get("/items")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items", "sort", "paging"));
    }

    @Test
    @DisplayName("Successful getting Item List with search and sort")
    void getItemList_shouldReturnItemListWithSearchAndSort() throws Exception {
        when(itemService.getItemList(any(ItemListSearchRequest.class))).thenReturn(testItemListResponse);

        mockMvc.perform(get("/items")
                        .param("search", "test")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    @DisplayName("Successful change item count")
    void changeItemCountFromItemPage_shouldChangeItemCountOnItemPage() throws Exception {
        when(itemService.changeItemCountFromItemPage(1L, Action.PLUS)).thenReturn(testItemDto);

        mockMvc.perform(post("/items/{id}", 1L)
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));
    }

    @Test
    @DisplayName("Successful change of the product quantity on the list page")
    void shouldChangeItemCountOnItemListPage() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?sort=NO&pageNumber=1&pageSize=5"));
    }

    @Test
    @DisplayName("Successful saving of search parameters during redirect")
    void shouldPreserveSearchParamsOnRedirect() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "MINUS")
                        .param("search", "test")
                        .param("sort", "PRICE")
                        .param("pageNumber", "2")
                        .param("pageSize", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=test&sort=PRICE&pageNumber=2&pageSize=10"));
    }
}