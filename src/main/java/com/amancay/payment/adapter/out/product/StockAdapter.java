package com.amancay.payment.adapter.out.product;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.payment.application.port.out.StockPort;
import com.amancay.repository.ProductVariantRepository;

@Component
class StockAdapter implements StockPort {

    private final ProductVariantRepository productVariantRepository;

    StockAdapter(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    // El UPDATE solo afecta la fila si hay stock suficiente, asi dos pagos concurrentes
    // no pueden dejarlo en negativo.
    @Override
    public boolean decrement(UUID productVariantId, int quantity) {
        return productVariantRepository.decrementStock(productVariantId, quantity) > 0;
    }
}
