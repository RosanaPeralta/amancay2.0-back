package com.amancay.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.entity.ReviewStatus;

public record ReviewDto(
        UUID id,
        UUID productId,
        UUID userId,
        int rating,
        String title,
        String comment,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
