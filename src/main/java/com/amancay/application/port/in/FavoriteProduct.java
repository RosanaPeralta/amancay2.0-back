package com.amancay.application.port.in;

import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.ProductSummary;

// Favorito con lo minimo del producto para dibujar la tarjeta.
public record FavoriteProduct(Favorite favorite, ProductSummary product) {
}
