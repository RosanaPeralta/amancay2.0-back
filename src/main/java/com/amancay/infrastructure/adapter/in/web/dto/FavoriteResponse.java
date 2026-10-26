package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.application.port.in.FavoriteProduct;

/**
 * Favorito del usuario. Los campos planos se mantienen por compatibilidad; {@code product} trae el
 * producto completo (precio, imagenes, descuento) para dibujar la misma tarjeta del catalogo.
 */
public record FavoriteResponse(
        UUID productId,
        String name,
        String slug,
        boolean active,
        Instant createdAt,
        ProductResponse product) {

    public static FavoriteResponse from(FavoriteProduct favorite) {
        return new FavoriteResponse(favorite.favorite().getProductId(), favorite.product().getName(),
                favorite.product().getSlug(), favorite.product().isActive(), favorite.favorite().getCreatedAt(),
                ProductResponse.from(favorite.product()));
    }
}
