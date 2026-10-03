package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Product;

public record CreateProductCommand(String name, String shortDescription, String description, boolean active,
        List<Product.NewVariant> variants, List<String> imageUrls, List<UUID> categoryIds, Long discountId) {
}
