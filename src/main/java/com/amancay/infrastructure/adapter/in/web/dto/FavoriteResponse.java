package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.application.port.in.FavoriteProduct;

/**
 * Favorito con lo mínimo del producto para dibujar la tarjeta. Se arma con su propio record y no
 * con {@code ProductSummaryResponse} para no depender del contrato de Productos.
 */
public record FavoriteResponse(
        UUID productId,
        String name,
        String slug,
        boolean active,
        Instant createdAt) {

    public static FavoriteResponse from(FavoriteProduct favorite) {
        return new FavoriteResponse(favorite.favorite().getProductId(), favorite.product().name(),
                favorite.product().slug(), favorite.product().active(), favorite.favorite().getCreatedAt());
    }
}
