package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.amancay.domain.model.ProductVariant;

public record ProductVariantResponse(UUID id, BigDecimal price, int stockQuantity) {

    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(variant.getId(), variant.getPrice(), variant.getStockQuantity());
    }
}
