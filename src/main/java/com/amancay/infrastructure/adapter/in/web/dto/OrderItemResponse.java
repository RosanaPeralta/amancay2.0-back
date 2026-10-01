package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.amancay.domain.model.OrderItem;

public record OrderItemResponse(UUID id, UUID productVariantId, int quantity, BigDecimal unitPrice,
        BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.id(), item.productVariantId(), item.quantity(), item.unitPrice(),
                item.subtotal());
    }
}
