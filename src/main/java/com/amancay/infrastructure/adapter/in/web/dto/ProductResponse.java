package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Product;

public record ProductResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        List<UUID> categoryIds,
        DiscountResponse discount) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getSlug(),
                product.getShortDescription(), product.getDescription(), product.isActive(), product.getCreatedAt(),
                product.getUpdatedAt(), product.getVariants().stream().map(ProductVariantResponse::from).toList(),
                product.getImages().stream().map(ProductImageResponse::from).toList(),
                List.copyOf(product.getCategoryIds()), DiscountResponse.from(product.getDiscount()));
    }
}
