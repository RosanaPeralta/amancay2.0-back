package com.amancay.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Producto marcado como favorito por un usuario (USR-08). Es un par (usuario, producto) sin más
 * estado; la unicidad la garantiza {@code uq_favorite_user_product} y {@code FavoriteService}.
 */
@Entity
@Table(name = "user_favorites")
@Getter
public class Favorite {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() {
    }

    public static Favorite of(UUID userId, UUID productId) {
        Favorite favorite = new Favorite();
        favorite.id = UUID.randomUUID();
        favorite.userId = Objects.requireNonNull(userId, "userId is required");
        favorite.productId = Objects.requireNonNull(productId, "productId is required");
        return favorite;
    }
}
