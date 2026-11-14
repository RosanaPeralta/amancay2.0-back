package com.amancay.application.port.in;

import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.Product;

// Favorito con el producto completo (precio, imagenes) para dibujar la misma tarjeta del catalogo.
public record FavoriteProduct(Favorite favorite, Product product) {
}
