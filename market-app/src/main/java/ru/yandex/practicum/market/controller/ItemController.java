package ru.yandex.practicum.market.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.dto.ItemChangeCountRequest;
import ru.yandex.practicum.market.dto.ItemListSearchRequest;
import ru.yandex.practicum.market.enums.Action;
import ru.yandex.practicum.market.model.CustomUserDetails;
import ru.yandex.practicum.market.service.ItemService;

@Slf4j
@Validated
@Controller
@AllArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItemById(@PathVariable @Positive(message = "Id must be greater than 0") Long id,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        log.debug("Getting item by ID {}.", id);
        Long userId = user == null ? 0L : user.getId();
        return itemService.getItemById(id, userId)
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .build());
    }

    @GetMapping({"/", "/items"})
    public Mono<Rendering> getItemList(@Valid @ModelAttribute ItemListSearchRequest req,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        log.info("Getting the list of items.");
        Long userId = user == null ? 0L : user.getId();
        return itemService.getItemList(req, userId)
                .map(response -> Rendering.view("items")
                        .modelAttribute("items", response.splittedItemDtoList())
                        .modelAttribute("search", req.search())
                        .modelAttribute("sort", req.sort())
                        .modelAttribute("paging", response.paging())
                        .build());
    }

    @PostMapping("/items/{id}")
    public Mono<Rendering> changeItemCountFromItemPage(@PathVariable @Positive(message = "Id must be greater than 0")
                                                       Long id,
                                                       @RequestParam Action action,
                                                       @AuthenticationPrincipal CustomUserDetails user) {
        log.info("Changing count of item with ID {}.", id);
        return itemService.changeItemCountFromItemPage(id, action, user.getId())
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .build());
    }

    @PostMapping("/items")
    public Mono<Rendering> changeItemCountFromItemListPage(@Valid @ModelAttribute ItemChangeCountRequest req,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        log.info("Changing count of item from item list page.");
        return itemService.changeItemCountFromItemPage(req.id(), req.action(), user.getId())
                .thenReturn(Rendering.redirectTo(buildItemsUrl(req)).build());
    }

    private String buildItemsUrl(ItemChangeCountRequest req) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/items")
                .queryParam("pageNumber", req.pageNumber())
                .queryParam("pageSize", req.pageSize());

        if (req.search() != null && !req.search().isBlank()) {
            builder.queryParam("search", req.search());
        }
        if (req.sort() != null) {
            builder.queryParam("sort", req.sort());
        }
        return builder.build().toUriString();
    }
}