package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.UUID;

import com.amancay.domain.model.ProductImage;

public record ProductImageResponse(UUID id, String imageUrl) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getImageUrl());
    }
}
