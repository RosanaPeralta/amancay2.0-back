package com.amancay.infrastructure.adapters.out.persistence.mapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amancay.domain.model.Product;
import com.amancay.entity.Discount;
import com.amancay.entity.ProductImage;
import com.amancay.entity.ProductVariant;

public final class ProductPersistenceMapper {
    private ProductPersistenceMapper() {
    }

    public static Product toDomain(com.amancay.entity.Product product) {
        Product.Discount discount = product.getDiscount() == null ? null
                : new Product.Discount(product.getDiscount().getId(), product.getDiscount().getPercentage(),
                        product.getDiscount().getDescription());
        List<Product.Variant> variants = product.getVariants().stream()
                .map(variant -> new Product.Variant(variant.getId(), variant.getPrice(), variant.getStockQuantity()))
                .toList();
        List<Product.Image> images = product.getImages().stream()
                .map(image -> new Product.Image(image.getId(), image.getImageUrl())).toList();
        Set<java.util.UUID> categoryIds = product.getCategoryIds().stream().collect(Collectors.toSet());
        return new Product(product.getId(), product.getName(), product.getSlug(), product.getShortDescription(),
                product.getDescription(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt(),
                variants, images, categoryIds, discount);
    }

    public static com.amancay.entity.Product toPersistence(Product product,
            com.amancay.entity.Product entity, Discount discount) {
        entity.setName(product.name());
        entity.setSlug(product.slug());
        entity.setShortDescription(product.shortDescription());
        entity.setDescription(product.description());
        entity.setActive(product.active());
        entity.setCategoryIds(new java.util.HashSet<>(product.categoryIds()));
        entity.setDiscount(discount);
        syncVariants(entity, product.variants());
        syncImages(entity, product.images());
        return entity;
    }

    private static void syncVariants(com.amancay.entity.Product entity, List<Product.Variant> variants) {
        Set<UUID> requestedIds = variants.stream().map(Product.Variant::id).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        entity.getVariants().removeIf(existing -> existing.getId() != null && !requestedIds.contains(existing.getId()));
        for (Product.Variant variant : variants) {
            ProductVariant current = variant.id() == null ? null : entity.getVariants().stream()
                    .filter(existing -> variant.id().equals(existing.getId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Variant does not belong to product: " + variant.id()));
            if (current == null) {
                current = new ProductVariant();
                entity.addVariant(current);
            }
            current.setPrice(variant.price());
            current.setStockQuantity(variant.stockQuantity());
        }
    }

    private static void syncImages(com.amancay.entity.Product entity, List<Product.Image> images) {
        Set<UUID> requestedIds = images.stream().map(Product.Image::id).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        entity.getImages().removeIf(existing -> existing.getId() != null && !requestedIds.contains(existing.getId()));
        for (Product.Image image : images) {
            ProductImage current = image.id() == null ? null : entity.getImages().stream()
                    .filter(existing -> image.id().equals(existing.getId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Image does not belong to product: " + image.id()));
            if (current == null) {
                current = new ProductImage();
                entity.addImage(current);
            }
            current.setImageUrl(image.imageUrl());
        }
    }
}