package com.amancay.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

// unitPrice es una copia del precio de la variante al momento de la compra: el
// precio del producto puede cambiar despues y el pedido tiene que conservar el
// valor que efectivamente se cobro.
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
