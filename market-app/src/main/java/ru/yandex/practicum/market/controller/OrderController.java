package ru.yandex.practicum.market.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.model.CustomUserDetails;
import ru.yandex.practicum.market.service.OrderService;

@Slf4j
@Validated
@Controller
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<Rendering> getOrderList(@AuthenticationPrincipal CustomUserDetails user) {
        log.info("Getting the list of orders for user {}.", user.getId());
        return orderService.getOrderList(user.getId())
                .collectList()
                .map(orders -> Rendering.view("orders")
                        .modelAttribute("orders", orders)
                        .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderById(@PathVariable @Positive(message = "Id must be greater than 0") Long id,
                                        @RequestParam(defaultValue = "false") boolean newOrder,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        log.info("Getting order by ID {} for user {}.", id, user.getId());
        return orderService.getOrderById(id, user.getId())
                .map(order -> Rendering.view("order")
                        .modelAttribute("order", order)
                        .modelAttribute("newOrder", newOrder)
                        .build());
    }

    @PostMapping("/buy")
    public Mono<Rendering> createOrder(@AuthenticationPrincipal CustomUserDetails user) {
        log.info("Creating a new Order for user {}.", user.getId());
        return orderService.createOrder(user.getId())
                .map(order -> Rendering.redirectTo(
                        "/orders/" + order.id() + "?newOrder=true").build());
    }
}