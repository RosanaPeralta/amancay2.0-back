package com.amancay.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
        List<UUID> categoryIds) {

    public record VariantRequest(
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int stockQuantity) {
    }
}
