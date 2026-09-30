package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProductSummary(UUID id, String name, String slug, boolean active, Instant createdAt, Instant updatedAt) {
}