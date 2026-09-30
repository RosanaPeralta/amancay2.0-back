package com.amancay.infrastructure.adapters.out.payments;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.domain.ports.out.PaymentStockPort;
import com.amancay.infrastructure.adapters.out.persistence.repository.ProductVariantRepository;

@Component
public class ProductVariantStockAdapter implements PaymentStockPort {
    private final ProductVariantRepository repository;

    public ProductVariantStockAdapter(ProductVariantRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean decrement(UUID variantId, int quantity) {
        return repository.decrementStock(variantId, quantity) != 0;
    }
}