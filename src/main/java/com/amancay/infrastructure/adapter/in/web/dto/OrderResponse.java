package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// Mismos campos que el antiguo OrderDto: el JSON que recibe el front no cambia.
public record OrderResponse(
        UUID id,
        UUID userId,
        UUID shippingAddressId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getShippingAddressId(), order.getStatus(),
                order.getSubtotal(), order.getShippingCost(), order.getTotal(), order.getCreatedAt(),
                order.getUpdatedAt(), order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
