package com.amancay.order.adapter.out.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.order.application.port.out.LoadProductVariantPort;
import com.amancay.repository.ProductVariantRepository;

@Component
class ProductVariantAdapter implements LoadProductVariantPort {

    private final ProductVariantRepository productVariantRepository;

    ProductVariantAdapter(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    public Optional<ProductVariantInfo> findById(UUID id) {
        return productVariantRepository.findById(id)
                .map(variant -> new ProductVariantInfo(variant.getId(), variant.getPrice(), variant.getStockQuantity()));
    }
}
