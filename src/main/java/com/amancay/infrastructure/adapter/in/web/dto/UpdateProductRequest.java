package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.amancay.application.port.in.UpdateProductCommand;
import com.amancay.domain.model.Product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        String shortDescription,
        String description,
        boolean active,
        @Valid List<VariantRequest> variants,
        @Valid List<ImageRequest> images,
        List<UUID> categoryIds,
        Long discountId) {

    public record VariantRequest(
            UUID id,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int stockQuantity) {
    }

    public record ImageRequest(
            UUID id,
            @NotBlank String imageUrl) {
    }

    public UpdateProductCommand toCommand() {
        return new UpdateProductCommand(name, shortDescription, description, active,
                variants == null ? null
                        : variants.stream().map(v -> new Product.VariantChange(v.id(), v.price(), v.stockQuantity()))
                                .toList(),
                images == null ? null
                        : images.stream().map(i -> new Product.ImageChange(i.id(), i.imageUrl())).toList(),
                categoryIds, discountId);
    }
}
