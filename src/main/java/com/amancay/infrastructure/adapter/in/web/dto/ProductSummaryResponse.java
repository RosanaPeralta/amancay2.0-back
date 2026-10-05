package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.ProductSummary;

public record ProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductSummaryResponse from(ProductSummary product) {
        return new ProductSummaryResponse(product.id(), product.name(), product.slug(), product.active(),
                product.createdAt(), product.updatedAt());
    }
}
