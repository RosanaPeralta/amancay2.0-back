package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<Variant> variants,
        List<Image> images,
        Set<UUID> categoryIds,
        Discount discount) {

    public Product {
        variants = variants == null ? List.of() : List.copyOf(variants);
        images = images == null ? List.of() : List.copyOf(images);
        categoryIds = categoryIds == null ? Set.of() : Set.copyOf(categoryIds);
    }

    public record Variant(UUID id, BigDecimal price, int stockQuantity) {
    }

    public record Image(UUID id, String imageUrl) {
    }

    public record Discount(Long id, BigDecimal percentage, String description) {
    }
}