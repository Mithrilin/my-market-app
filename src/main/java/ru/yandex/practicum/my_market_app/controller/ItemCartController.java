package ru.yandex.practicum.my_market_app.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.my_market_app.dto.ItemCartListResponse;
import ru.yandex.practicum.my_market_app.enums.Action;
import ru.yandex.practicum.my_market_app.service.ItemService;

@Slf4j
@Validated
@Controller
@AllArgsConstructor
@RequestMapping("/cart/items")
public class ItemCartController {

    private final ItemService itemService;

    @GetMapping
    public String getItemCartList(Model model) {
        log.debug("Getting cart of items");
        ItemCartListResponse itemCartListResponse = itemService.getItemCartList();
        model.addAttribute("items", itemCartListResponse.itemDtoList());
        model.addAttribute("total", itemCartListResponse.total());
        return "cart";
    }

    @PostMapping
    public String changeItemCountFromCartPage(@RequestParam @Positive(message = "Id must be greater than 0") Long id,
                                              @RequestParam Action action,
                                              Model model) {
        log.info("Changing count of item with ID {} from cart.", id);
        ItemCartListResponse itemCartListResponse = itemService.changeItemCountFromCartPage(id, action);
        model.addAttribute("items", itemCartListResponse.itemDtoList());
        model.addAttribute("total", itemCartListResponse.total());
        return "cart";
    }
}
