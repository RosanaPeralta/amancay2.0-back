package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;

@Getter
public class Product {

    private final UUID id;
    private String name;
    // Version amigable para la url del producto; la genera el caso de uso a partir del nombre.
    private String slug;
    private String shortDescription;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;
    private List<ProductVariant> variants;
    private List<ProductImage> images;
    private Set<UUID> categoryIds;
    private Discount discount;

    // Reconstruye un producto ya existente (lo usa el adaptador de persistencia).
    public Product(UUID id, String name, String slug, String shortDescription, String description, boolean active,
            Instant createdAt, Instant updatedAt, List<ProductVariant> variants, List<ProductImage> images,
            Set<UUID> categoryIds, Discount discount) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.variants = new ArrayList<>(variants);
        this.images = new ArrayList<>(images);
        this.categoryIds = new HashSet<>(categoryIds);
        this.discount = discount;
    }

    public static Product create(String name, String slug, String shortDescription, String description,
            boolean active, List<NewVariant> variants, List<String> imageUrls, Collection<UUID> categoryIds) {
        List<ProductVariant> newVariants = variants == null ? List.of()
                : variants.stream().map(v -> new ProductVariant(null, v.price(), v.stockQuantity())).toList();
        List<ProductImage> newImages = imageUrls == null ? List.of()
                : imageUrls.stream().map(url -> new ProductImage(null, url)).toList();
        return new Product(null, name, slug, shortDescription, description, active, null, null, newVariants,
                newImages, categoryIds == null ? Set.of() : Set.copyOf(categoryIds), null);
    }

    public void updateDetails(String name, String slug, String shortDescription, String description, boolean active,
            Collection<UUID> categoryIds) {
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.active = active;
        this.categoryIds = categoryIds == null ? new HashSet<>() : new HashSet<>(categoryIds);
    }

    // Las variantes con id se actualizan (y tienen que ser de este producto), las que vienen
    // sin id se agregan y las existentes que no vienen se eliminan.
    public void replaceVariants(List<VariantChange> changes) {
        List<VariantChange> requested = changes == null ? List.of() : changes;
        Set<UUID> keptIds = new HashSet<>();
        List<ProductVariant> added = new ArrayList<>();
        for (VariantChange change : requested) {
            if (change.id() == null) {
                added.add(new ProductVariant(null, change.price(), change.stockQuantity()));
            } else {
                keptIds.add(change.id());
                variants.stream()
                        .filter(existing -> change.id().equals(existing.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Variant does not belong to product: " + change.id()))
                        .update(change.price(), change.stockQuantity());
            }
        }
        List<ProductVariant> result = new ArrayList<>(
                variants.stream().filter(variant -> variant.getId() == null || keptIds.contains(variant.getId())).toList());
        result.addAll(added);
        this.variants = result;
    }

    // Misma regla que replaceVariants, para las imagenes.
    public void replaceImages(List<ImageChange> changes) {
        List<ImageChange> requested = changes == null ? List.of() : changes;
        Set<UUID> keptIds = new HashSet<>();
        List<ProductImage> added = new ArrayList<>();
        for (ImageChange change : requested) {
            if (change.id() == null) {
                added.add(new ProductImage(null, change.imageUrl()));
            } else {
                keptIds.add(change.id());
                images.stream()
                        .filter(existing -> change.id().equals(existing.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Image does not belong to product: " + change.id()))
                        .changeUrl(change.imageUrl());
            }
        }
        List<ProductImage> result = new ArrayList<>(
                images.stream().filter(image -> image.getId() == null || keptIds.contains(image.getId())).toList());
        result.addAll(added);
        this.images = result;
    }

    public void assignDiscount(Discount discount) {
        this.discount = discount;
    }

    public void removeDiscount() {
        this.discount = null;
    }

    public List<ProductVariant> getVariants() {
        return List.copyOf(variants);
    }

    public List<ProductImage> getImages() {
        return List.copyOf(images);
    }

    public Set<UUID> getCategoryIds() {
        return Set.copyOf(categoryIds);
    }

    public ProductSummary toSummary() {
        return new ProductSummary(id, name, slug, active, createdAt, updatedAt);
    }

    public record NewVariant(BigDecimal price, int stockQuantity) {
    }

    public record VariantChange(UUID id, BigDecimal price, int stockQuantity) {
    }

    public record ImageChange(UUID id, String imageUrl) {
    }
}
