package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;

// productId/productName/imageUrl son null si la variante ya no existe.
public record OrderItemResponse(UUID id, UUID productVariantId, UUID productId, String productName,
        String imageUrl, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem item, Map<UUID, OrderItemProduct> products) {
        OrderItemProduct product = products.get(item.productVariantId());
        return new OrderItemResponse(item.id(), item.productVariantId(),
                product == null ? null : product.productId(),
                product == null ? null : product.name(),
                product == null ? null : product.imageUrl(),
                item.quantity(), item.unitPrice(), item.subtotal());
    }
}
