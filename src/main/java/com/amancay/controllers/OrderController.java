package com.amancay.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.CreateOrderRequest;
import com.amancay.dto.OrderDto;
import com.amancay.dto.UpdateOrderStatusRequest;
import com.amancay.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> list(
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(orderService.listOrders(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getById(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(orderService.getOrder(id, userId));
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDto> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.changeStatus(id, request.status()));
    }
}
