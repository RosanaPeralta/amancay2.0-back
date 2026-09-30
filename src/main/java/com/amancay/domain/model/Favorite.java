package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Favorite(UUID id, UUID userId, UUID productId, Instant createdAt) {
}