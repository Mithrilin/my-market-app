package ru.yandex.practicum.market.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.CartPageResponse;
import ru.yandex.practicum.market.enums.Action;
import ru.yandex.practicum.market.service.ItemService;

@Slf4j
@Validated
@Controller
@AllArgsConstructor
@RequestMapping("/cart/items")
public class ItemCartController {

    private final ItemService itemService;

    @GetMapping
    public Mono<Rendering> getItemCartList() {
        log.debug("Getting cart of items");
        return itemService.getItemCartList()
                .map(this::toCartView);
    }

    @PostMapping
    public Mono<Rendering> changeItemCountFromCartPage(@RequestParam @Positive(message = "Id must be greater than 0")
                                                       Long id,
                                                       @RequestParam Action action) {
        log.info("Changing count of item with ID {} from cart.", id);
        return itemService.changeItemCountFromCartPage(id, action)
                .map(this::toCartView);
    }

    private Rendering toCartView(CartPageResponse r) {
        return Rendering.view("cart")
                .modelAttribute("items", r.itemDtoList())
                .modelAttribute("total", r.total())
                .modelAttribute("paymentAvailable", r.paymentAvailable())
                .modelAttribute("paymentMessage", r.paymentMessage())
                .build();
    }
}
