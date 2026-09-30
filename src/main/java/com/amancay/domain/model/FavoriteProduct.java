package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public record FavoriteProduct(UUID productId, String name, String slug, boolean active, Instant createdAt) {
}