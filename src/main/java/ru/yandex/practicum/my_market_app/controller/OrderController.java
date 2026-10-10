package ru.yandex.practicum.my_market_app.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.my_market_app.dto.OrderDto;
import ru.yandex.practicum.my_market_app.service.OrderService;

import java.util.List;

@Slf4j
@Validated
@Controller
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping({"/orders"})
    public String getOrderList(Model model) {
        log.info("Getting the list of orders.");
        List<OrderDto> orderDtoList = orderService.getOrderList();
        model.addAttribute("orders", orderDtoList);
        return "orders";
    }

    @GetMapping({"/orders/{id}"})
    public String getOrderById(@PathVariable @Positive(message = "Id must be greater than 0") Long id,
                               @RequestParam(defaultValue = "false") boolean newOrder,
                               Model model) {
        log.info("Getting order by ID {}.", id);
        OrderDto orderDto = orderService.getOrderById(id);
        model.addAttribute("order", orderDto);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    @PostMapping("/buy")
    public String createOrder(RedirectAttributes redirectAttributes) {
        log.info("Creating a new Order");
        OrderDto orderDto = orderService.createOrder();
        redirectAttributes.addAttribute("newOrder", "true");
        return "redirect:/orders/" + orderDto.id();
    }
}
