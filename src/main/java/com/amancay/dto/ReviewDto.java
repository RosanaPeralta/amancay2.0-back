package com.amancay.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.entity.ReviewStatus;

/**
 * {@code authorName} es el nombre del autor tal como está en {@code users.name}; puede ser
 * {@code null} si el usuario nunca lo cargó. Nunca se expone el email.
 */
public record ReviewDto(
        UUID id,
        UUID productId,
        UUID userId,
        String authorName,
        int rating,
        String title,
        String comment,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
