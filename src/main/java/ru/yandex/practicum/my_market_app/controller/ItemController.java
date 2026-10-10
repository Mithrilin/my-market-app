package ru.yandex.practicum.my_market_app.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.my_market_app.dto.ItemChangeCountRequest;
import ru.yandex.practicum.my_market_app.dto.ItemDto;
import ru.yandex.practicum.my_market_app.dto.ItemListResponse;
import ru.yandex.practicum.my_market_app.dto.ItemListSearchRequest;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.service.ItemService;

@Slf4j
@Validated
@Controller
@AllArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/{id}")
    public String getItemById(@PathVariable @Positive(message = "Id must be greater than 0") Long id,
                              Model model) {
        log.debug("Getting item by ID {}.", id);
        ItemDto itemDto = itemService.getItemById(id);
        model.addAttribute("item", itemDto);
        return "item";
    }

    @GetMapping({"/", "/items"})
    public String getItemList(@Valid @ModelAttribute ItemListSearchRequest itemListSearchRequest,
                              Model model) {
        log.info("Getting the list of items.");
        ItemListResponse itemListResponse = itemService.getItemList(itemListSearchRequest);
        model.addAttribute("items", itemListResponse.splittedItemDtoList());
        model.addAttribute("search", itemListSearchRequest.search());
        model.addAttribute("sort", itemListSearchRequest.sort());
        model.addAttribute("paging", itemListResponse.paging());
        return "items";
    }

    @PostMapping("/items/{id}")
    public String changeItemCountFromItemPage(@PathVariable @Positive(message = "Id must be greater than 0") Long id,
                                              @RequestParam Action action,
                                              Model model) {
        log.info("Changing count of item with ID {}.", id);
        ItemDto itemDto = itemService.changeItemCountFromItemPage(id, action);
        model.addAttribute("item", itemDto);
        return "item";
    }

    @PostMapping("/items")
    public String changeItemCountFromItemListPage(@Valid @ModelAttribute
                                                  ItemChangeCountRequest itemChangeCountRequest,
                                                  RedirectAttributes redirectAttributes) {
        log.info("Changing count of item from item list page.");
        itemService.changeItemCountFromItemPage(itemChangeCountRequest.id(), itemChangeCountRequest.action());

        if (itemChangeCountRequest.search() != null) {
            redirectAttributes.addAttribute("search", itemChangeCountRequest.search());
        }
        if (itemChangeCountRequest.sort() != null) {
            redirectAttributes.addAttribute("sort", itemChangeCountRequest.sort());
        }

        redirectAttributes.addAttribute("pageNumber", itemChangeCountRequest.pageNumber());
        redirectAttributes.addAttribute("pageSize", itemChangeCountRequest.pageSize());
        return "redirect:/items";
    }
}
