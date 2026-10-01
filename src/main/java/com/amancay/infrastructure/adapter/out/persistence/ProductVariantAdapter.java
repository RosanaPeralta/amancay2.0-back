package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.LoadProductVariantPort;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataProductVariantRepository;

@Component
class ProductVariantAdapter implements LoadProductVariantPort {

    private final SpringDataProductVariantRepository productVariantRepository;

    ProductVariantAdapter(SpringDataProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    public Optional<ProductVariantInfo> findById(UUID id) {
        return productVariantRepository.findById(id)
                .map(variant -> new ProductVariantInfo(variant.getId(), variant.getPrice(), variant.getStockQuantity()));
    }
}
