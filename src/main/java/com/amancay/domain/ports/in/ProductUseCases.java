package com.amancay.domain.ports.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductPage;

public interface ProductUseCases {
    Product create(CreateProduct command);

    Product update(UUID id, UpdateProduct command);

    Product getById(UUID id);

    ProductPage list(ProductQuery query);

    void delete(UUID id);

    Product assignDiscount(UUID productId, Long discountId);

    Product removeDiscount(UUID productId);

    record CreateProduct(
            String name,
            String shortDescription,
            String description,
            boolean active,
            List<VariantInput> variants,
            List<String> imageUrls,
            Set<UUID> categoryIds,
            Long discountId) {
    }

    record UpdateProduct(
            String name,
            String shortDescription,
            String description,
            boolean active,
            List<VariantUpdate> variants,
            List<ImageUpdate> images,
            Set<UUID> categoryIds,
            Long discountId) {
    }

    record VariantInput(BigDecimal price, int stockQuantity) {
    }

    record VariantUpdate(UUID id, BigDecimal price, int stockQuantity) {
    }

    record ImageUpdate(UUID id, String imageUrl) {
    }

    record ProductQuery(int page, int size, String name, UUID categoryId, Boolean active, List<SortOrder> sort) {
    }

    record SortOrder(String property, boolean ascending) {
    }
}