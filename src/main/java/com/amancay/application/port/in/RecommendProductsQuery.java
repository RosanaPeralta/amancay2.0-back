package com.amancay.application.port.in;

import java.util.UUID;

public interface RecommendProductsQuery {
    // Productos activos que el usuario todavia no marco, ordenados por afinidad con sus favoritos.
    ProductRecommendations recommend(UUID userId, int limit);
}
