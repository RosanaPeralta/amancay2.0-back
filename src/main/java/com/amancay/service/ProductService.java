package com.amancay.service;

import java.util.HashSet;
import java.util.List;
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
import com.amancay.dto.ProductSummaryDto;
import com.amancay.dto.ProductVariantDto;
import com.amancay.dto.UpdateProductRequest;
import com.amancay.entity.Product;
import com.amancay.entity.ProductVariant;
import com.amancay.exceptions.DuplicateSlugException;
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
        validateSlug(request.slug(), null);
        Product product = new Product();
        applyProductFields(product, request.name(), request.slug(), request.shortDescription(), request.description(), request.active());
        if (request.variants() != null) {
            request.variants().forEach(variantRequest -> {
                ProductVariant variant = new ProductVariant();
                variant.setPrice(variantRequest.price());
                variant.setStockQuantity(variantRequest.stockQuantity());
                product.addVariant(variant);
            });
        }
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findProduct(id);
        validateSlug(request.slug(), id);
        applyProductFields(product, request.name(), request.slug(), request.shortDescription(), request.description(), request.active());

        List<UpdateProductRequest.VariantRequest> requestedVariants = request.variants() == null ? List.of() : request.variants();
        Set<UUID> requestedIds = new HashSet<>();
        for (UpdateProductRequest.VariantRequest variantRequest : requestedVariants) {
            ProductVariant variant;
            if (variantRequest.id() == null) {
                variant = new ProductVariant();
                product.addVariant(variant);
            } else {
                requestedIds.add(variantRequest.id());
                variant = product.getVariants().stream()
                        .filter(existing -> variantRequest.id().equals(existing.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Variant does not belong to product: " + variantRequest.id()));
            }
            variant.setPrice(variantRequest.price());
            variant.setStockQuantity(variantRequest.stockQuantity());
        }
        product.getVariants().removeIf(variant -> variant.getId() != null && !requestedIds.contains(variant.getId()));
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
                product.getId(), product.getName(), product.getSlug(), product.isActive())).getContent(),
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

    private void validateSlug(String slug, UUID currentId) {
        boolean exists = currentId == null ? productRepository.existsBySlug(slug) : productRepository.existsBySlugAndIdNot(slug, currentId);
        if (exists) {
            throw new DuplicateSlugException(slug);
        }
    }

    private void applyProductFields(Product product, String name, String slug, String shortDescription, String description, boolean active) {
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
        return new ProductDto(product.getId(), product.getName(), product.getSlug(), product.getShortDescription(),
                product.getDescription(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt(), variants);
    }
}
