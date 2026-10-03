package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.amancay.application.port.in.CreateProductCommand;
import com.amancay.domain.model.Product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        String shortDescription,
        String description,
        boolean active,
        @Valid List<VariantRequest> variants,
        List<@NotBlank String> imageUrls,
        List<UUID> categoryIds,
        Long discountId) {

    public record VariantRequest(
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int stockQuantity) {
    }

    public CreateProductCommand toCommand() {
        return new CreateProductCommand(name, shortDescription, description, active,
                variants == null ? null
                        : variants.stream().map(v -> new Product.NewVariant(v.price(), v.stockQuantity())).toList(),
                imageUrls, categoryIds, discountId);
    }
}
