package com.amancay.infrastructure.adapters.in.rest.mapper;

import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductSummary;
import com.amancay.dto.DiscountResponse;
import com.amancay.dto.ProductDto;
import com.amancay.dto.ProductImageDto;
import com.amancay.dto.ProductSummaryDto;
import com.amancay.dto.ProductVariantDto;

public final class ProductApiMapper {
    private ProductApiMapper() {
    }

    public static ProductDto toDto(Product product) {
        DiscountResponse discount = product.discount() == null ? null
                : new DiscountResponse(product.discount().id(), product.discount().percentage(),
                        product.discount().description());
        return new ProductDto(product.id(), product.name(), product.slug(), product.shortDescription(),
                product.description(), product.active(), product.createdAt(), product.updatedAt(),
                product.variants().stream().map(variant -> new ProductVariantDto(variant.id(), variant.price(),
                        variant.stockQuantity())).toList(),
                product.images().stream().map(image -> new ProductImageDto(image.id(), image.imageUrl())).toList(),
                product.categoryIds().stream().toList(), discount);
    }

    public static ProductSummaryDto toSummaryDto(ProductSummary product) {
        return new ProductSummaryDto(product.id(), product.name(), product.slug(), product.active(),
                product.createdAt(), product.updatedAt());
    }
}