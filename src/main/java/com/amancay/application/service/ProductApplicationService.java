package com.amancay.application.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductPage;
import com.amancay.domain.ports.in.ProductUseCases;
import com.amancay.domain.ports.out.DiscountLookupPort;
import com.amancay.domain.ports.out.ProductCatalogPort;
import com.amancay.exceptions.DiscountNotFoundException;
import com.amancay.exceptions.ProductNotFoundException;

public class ProductApplicationService implements ProductUseCases {
    private final ProductCatalogPort productCatalog;
    private final DiscountLookupPort discountLookup;

    public ProductApplicationService(ProductCatalogPort productCatalog, DiscountLookupPort discountLookup) {
        this.productCatalog = productCatalog;
        this.discountLookup = discountLookup;
    }

    @Override
    public Product create(CreateProduct command) {
        String slug = generateUniqueSlug(command.name(), null);
        List<Product.Variant> variants = command.variants() == null ? List.of()
                : command.variants().stream().map(variant -> new Product.Variant(null, variant.price(), variant.stockQuantity())).toList();
        List<Product.Image> images = command.imageUrls() == null ? List.of()
                : command.imageUrls().stream().map(url -> new Product.Image(null, url)).toList();
        Product product = new Product(null, command.name(), slug, command.shortDescription(), command.description(),
                command.active(), null, null, variants, images,
                command.categoryIds() == null ? Set.of() : new HashSet<>(command.categoryIds()),
                command.discountId() == null ? null : findDiscount(command.discountId()));
        return productCatalog.save(product);
    }

    @Override
    public Product update(UUID id, UpdateProduct command) {
        Product current = findProduct(id);
        String slug = generateUniqueSlug(command.name(), id);
        List<Product.Variant> variants = command.variants() == null ? List.of() : command.variants().stream()
                .map(variant -> {
                    if (variant.id() != null && current.variants().stream().noneMatch(existing -> variant.id().equals(existing.id()))) {
                        throw new IllegalArgumentException("Variant does not belong to product: " + variant.id());
                    }
                    return new Product.Variant(variant.id(), variant.price(), variant.stockQuantity());
                }).toList();
        List<Product.Image> images = command.images() == null ? List.of() : command.images().stream()
                .map(image -> {
                    if (image.id() != null && current.images().stream().noneMatch(existing -> image.id().equals(existing.id()))) {
                        throw new IllegalArgumentException("Image does not belong to product: " + image.id());
                    }
                    return new Product.Image(image.id(), image.imageUrl());
                }).toList();
        Product.Discount discount = command.discountId() == null ? current.discount() : findDiscount(command.discountId());
        Product updated = new Product(current.id(), command.name(), slug, command.shortDescription(), command.description(),
                command.active(), current.createdAt(), current.updatedAt(), variants, images,
                command.categoryIds() == null ? Set.of() : new HashSet<>(command.categoryIds()), discount);
        return productCatalog.save(updated);
    }

    @Override
    public Product getById(UUID id) {
        return findProduct(id);
    }

    @Override
    public ProductPage list(ProductQuery query) {
        return productCatalog.search(query);
    }

    @Override
    public void delete(UUID id) {
        productCatalog.delete(findProduct(id));
    }

    @Override
    public Product assignDiscount(UUID productId, Long discountId) {
        Product current = findProduct(productId);
        Product updated = new Product(current.id(), current.name(), current.slug(), current.shortDescription(),
                current.description(), current.active(), current.createdAt(), current.updatedAt(), current.variants(),
                current.images(), current.categoryIds(), findDiscount(discountId));
        return productCatalog.save(updated);
    }

    @Override
    public Product removeDiscount(UUID productId) {
        Product current = findProduct(productId);
        Product updated = new Product(current.id(), current.name(), current.slug(), current.shortDescription(),
                current.description(), current.active(), current.createdAt(), current.updatedAt(), current.variants(),
                current.images(), current.categoryIds(), null);
        return productCatalog.save(updated);
    }

    private Product findProduct(UUID id) {
        return productCatalog.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product.Discount findDiscount(Long id) {
        return discountLookup.findDiscountById(id).orElseThrow(() -> new DiscountNotFoundException(id));
    }

    private String generateUniqueSlug(String name, UUID currentId) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (currentId == null ? productCatalog.existsBySlug(candidate)
                : productCatalog.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? UUID.randomUUID().toString() : slug;
    }
}