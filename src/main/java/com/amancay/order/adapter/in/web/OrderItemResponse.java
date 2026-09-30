package com.amancay.order.adapter.in.web;

import java.math.BigDecimal;
import java.util.UUID;

import com.amancay.order.domain.model.OrderItem;

public record OrderItemResponse(UUID id, UUID productVariantId, int quantity, BigDecimal unitPrice,
        BigDecimal subtotal) {

    static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.id(), item.productVariantId(), item.quantity(), item.unitPrice(),
                item.subtotal());
    }
}
