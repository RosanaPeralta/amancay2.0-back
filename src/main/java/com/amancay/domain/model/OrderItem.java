package com.amancay.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

// unitPrice es el precio al momento de la compra.
public record OrderItem(UUID id, UUID productVariantId, int quantity, BigDecimal unitPrice) {

    public OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }

    public static OrderItem create(UUID productVariantId, int quantity, BigDecimal unitPrice) {
        return new OrderItem(null, productVariantId, quantity, unitPrice);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
