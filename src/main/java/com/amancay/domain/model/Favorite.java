package com.amancay.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Producto marcado como favorito por un usuario. Es un par (usuario, producto) sin más
 * estado; la unicidad la garantiza {@code uq_favorite_user_product} y el caso de uso.
 */
public class Favorite {

    private final UUID id;
    private final UUID userId;
    private final UUID productId;
    private final Instant createdAt;

    public Favorite(UUID id, UUID userId, UUID productId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public static Favorite of(UUID userId, UUID productId) {
        return new Favorite(UUID.randomUUID(), Objects.requireNonNull(userId, "userId is required"),
                Objects.requireNonNull(productId, "productId is required"), null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
