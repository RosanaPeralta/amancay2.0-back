package com.amancay.domain.model;

import java.util.UUID;

// Lo que se muestra de un producto en una linea de pedido: las lineas solo guardan la variante.
public record OrderItemProduct(UUID variantId, UUID productId, String name, String imageUrl) {
}
