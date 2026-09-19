package com.amancay.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.amancay.entity.OrderStatus;

public record OrderDto(
        UUID id,
        UUID userId,
        UUID shippingAddressId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemDto> items) {
}
