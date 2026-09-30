package com.amancay.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Favorito con lo mínimo del producto para dibujar la tarjeta. Se arma desde {@code Product}
 * directamente y no desde {@code ProductSummaryDto} para no depender del contrato de Productos.
 */
public record FavoriteDto(
        UUID productId,
        String name,
        String slug,
        boolean active,
        Instant createdAt) {
}
