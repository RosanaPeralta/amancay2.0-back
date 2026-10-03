package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.application.port.in.ReviewWithAuthor;
import com.amancay.domain.model.ReviewStatus;

/**
 * {@code authorName} es el nombre del autor tal como está en {@code users.name}; puede ser
 * {@code null} si el usuario nunca lo cargó. Nunca se expone el email.
 */
public record ReviewResponse(
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

    public static ReviewResponse from(ReviewWithAuthor item) {
        return new ReviewResponse(item.review().getId(), item.review().getProductId(), item.review().getUserId(),
                item.authorName(), item.review().getRating(), item.review().getTitle(), item.review().getComment(),
                item.review().getStatus(), item.review().getCreatedAt(), item.review().getUpdatedAt());
    }
}
