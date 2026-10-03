package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amancay.application.port.in.ListAllOrdersQuery.AdminOrder;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

// Los campos de OrderResponse mas el email del comprador, para el panel de admin.
public record AdminOrderResponse(
        UUID id,
        UUID userId,
        String buyerEmail,
        UUID shippingAddressId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items) {

    public static AdminOrderResponse from(AdminOrder adminOrder, Map<UUID, OrderItemProduct> products) {
        Order order = adminOrder.order();
        return new AdminOrderResponse(order.getId(), order.getUserId(), adminOrder.buyerEmail(),
                order.getShippingAddressId(), order.getStatus(), order.getSubtotal(), order.getShippingCost(),
                order.getTotal(), order.getCreatedAt(), order.getUpdatedAt(),
                order.getItems().stream().map(item -> OrderItemResponse.from(item, products)).toList());
    }
}
