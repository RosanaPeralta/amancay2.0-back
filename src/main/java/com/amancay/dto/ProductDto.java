package com.amancay.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<ProductVariantDto> variants,
        List<ProductImageDto> images,
        List<UUID> categoryIds) {
}
