package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.ChangeOrderStatusUseCase;
import com.amancay.application.port.in.CreateOrderUseCase;
import com.amancay.application.port.in.DescribeOrderItemsQuery;
import com.amancay.application.port.in.GetOrderQuery;
import com.amancay.application.port.in.ListOrdersQuery;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.infrastructure.adapter.in.web.dto.CreateOrderRequest;
import com.amancay.infrastructure.adapter.in.web.dto.OrderResponse;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateOrderStatusRequest;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final ListOrdersQuery listOrdersQuery;
    private final GetOrderQuery getOrderQuery;
    private final CreateOrderUseCase createOrderUseCase;
    private final ChangeOrderStatusUseCase changeOrderStatusUseCase;
    private final DescribeOrderItemsQuery describeOrderItemsQuery;

    public OrderController(ListOrdersQuery listOrdersQuery, GetOrderQuery getOrderQuery,
            CreateOrderUseCase createOrderUseCase, ChangeOrderStatusUseCase changeOrderStatusUseCase,
            DescribeOrderItemsQuery describeOrderItemsQuery) {
        this.listOrdersQuery = listOrdersQuery;
        this.getOrderQuery = getOrderQuery;
        this.createOrderUseCase = createOrderUseCase;
        this.changeOrderStatusUseCase = changeOrderStatusUseCase;
        this.describeOrderItemsQuery = describeOrderItemsQuery;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(required = false) UUID userId) {
        List<Order> orders = listOrdersQuery.list(loggedUser.id(), userId);
        Map<UUID, OrderItemProduct> products = describeOrderItemsQuery.describe(orders);
        return ResponseEntity.ok(orders.stream()
                .map(order -> OrderResponse.from(order, products))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(toResponse(getOrderQuery.get(loggedUser.id(), id, userId)));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = toResponse(createOrderUseCase.create(request.toCommand(loggedUser.id())));
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> changeStatus(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(toResponse(changeOrderStatusUseCase.changeStatus(loggedUser.id(), id,
                request.status())));
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.from(order, describeOrderItemsQuery.describe(List.of(order)));
    }
}
