package com.amancay.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.CreateProductRequest;
import com.amancay.dto.PageResponse;
import com.amancay.dto.ProductDto;
import com.amancay.dto.ProductImageDto;
import com.amancay.dto.ProductSummaryDto;
import com.amancay.dto.ProductVariantDto;
import com.amancay.dto.UpdateProductRequest;
import com.amancay.entity.Product;
import com.amancay.entity.ProductImage;
import com.amancay.entity.ProductVariant;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        Product product = new Product();

        applyProductFields(product, request.name(), generateUniqueSlug(request.name(), null), request.shortDescription(),
                request.description(), request.active());
        if (request.variants() != null) {
            request.variants().forEach(variantRequest -> {
                ProductVariant variant = new ProductVariant();
                variant.setPrice(variantRequest.price());
                variant.setStockQuantity(variantRequest.stockQuantity());
                product.addVariant(variant);
            });
        }
        if (request.imageUrls() != null) {
            request.imageUrls().forEach(imageUrl -> {
                ProductImage image = new ProductImage();
                image.setImageUrl(imageUrl);
                product.addImage(image);
            });
        }
        product.setCategoryIds(request.categoryIds() == null ? new HashSet<>() : new HashSet<>(request.categoryIds()));
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findProduct(id);
        applyProductFields(product, request.name(), generateUniqueSlug(request.name(), id), request.shortDescription(),
                request.description(), request.active());

        List<UpdateProductRequest.VariantRequest> requestedVariants = request.variants() == null ? List.of() : request.variants();
        Set<UUID> requestedVariantIds = new HashSet<>();
        for (UpdateProductRequest.VariantRequest variantRequest : requestedVariants) {
            ProductVariant variant;
            if (variantRequest.id() == null) {
                variant = new ProductVariant();
                product.addVariant(variant);
            } else {
                requestedVariantIds.add(variantRequest.id());
                variant = product.getVariants().stream()
                        .filter(existing -> variantRequest.id().equals(existing.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Variant does not belong to product: " + variantRequest.id()));
            }
            variant.setPrice(variantRequest.price());
            variant.setStockQuantity(variantRequest.stockQuantity());
        }
        product.getVariants().removeIf(variant -> variant.getId() != null && !requestedVariantIds.contains(variant.getId()));

        List<UpdateProductRequest.ImageRequest> requestedImages = request.images() == null ? List.of() : request.images();
        Set<UUID> requestedImageIds = new HashSet<>();
        for (UpdateProductRequest.ImageRequest imageRequest : requestedImages) {
            ProductImage image;
            if (imageRequest.id() == null) {
                image = new ProductImage();
                product.addImage(image);
            } else {
                requestedImageIds.add(imageRequest.id());
                image = product.getImages().stream()
                        .filter(existing -> imageRequest.id().equals(existing.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Image does not belong to product: " + imageRequest.id()));
            }
            image.setImageUrl(imageRequest.imageUrl());
        }
        product.getImages().removeIf(image -> image.getId() != null && !requestedImageIds.contains(image.getId()));

        product.setCategoryIds(request.categoryIds() == null ? new HashSet<>() : new HashSet<>(request.categoryIds()));

        return toDto(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID id) {
        return toDto(findProduct(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> listProducts(Pageable pageable, Optional<String> query, Optional<Boolean> active) {
        Page<Product> products;
        if (query.isPresent() && active.isPresent()) {
            String value = query.get();
            products = productRepository.searchByActive(active.get(), value, pageable);
        } else if (query.isPresent()) {
            String value = query.get();
            products = productRepository.searchByNameOrSlug(value, pageable);
        } else if (active.isPresent()) {
            products = productRepository.findByActive(active.get(), pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return new PageResponse<>(products.map(product -> new ProductSummaryDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
            )).getContent(),
                products.getNumber(), products.getSize(), products.getTotalElements(), products.getTotalPages());
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private String generateUniqueSlug(String name, UUID currentId) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (currentId == null ? productRepository.existsBySlug(candidate) : productRepository.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? UUID.randomUUID().toString() : slug;
    }

    private void applyProductFields(Product product, String name, String slug, String shortDescription, String description,
            boolean active) {
        product.setName(name);
        product.setSlug(slug);
        product.setShortDescription(shortDescription);
        product.setDescription(description);
        product.setActive(active);
    }

    private ProductDto toDto(Product product) {
        List<ProductVariantDto> variants = product.getVariants().stream()
                .map(variant -> new ProductVariantDto(variant.getId(), variant.getPrice(), variant.getStockQuantity()))
                .toList();
        List<ProductImageDto> images = product.getImages().stream()
                .map(image -> new ProductImageDto(image.getId(), image.getImageUrl()))
                .toList();
        return new ProductDto(product.getId(), product.getName(), product.getSlug(), product.getShortDescription(),
                product.getDescription(), product.isActive(),
                product.getCreatedAt(), product.getUpdatedAt(), variants, images, List.copyOf(product.getCategoryIds()));
    }
}
