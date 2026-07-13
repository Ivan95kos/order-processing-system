package com.ivankos.orderservice.controller;

import com.ivankos.orderservice.dto.CreateOrderRequest;
import com.ivankos.orderservice.dto.OrderResponse;
import com.ivankos.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest createOrderRequest) {
        return orderService.createOrder(createOrderRequest);
    }

    @GetMapping(path = "/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}
