package com.amancay.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.StockPort;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataProductVariantRepository;

@Component
class StockAdapter implements StockPort {

    private final SpringDataProductVariantRepository productVariantRepository;

    StockAdapter(SpringDataProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    // El UPDATE solo afecta la fila si hay stock suficiente, asi dos pagos concurrentes
    // no pueden dejarlo en negativo.
    @Override
    public boolean decrement(UUID productVariantId, int quantity) {
        return productVariantRepository.decrementStock(productVariantId, quantity) > 0;
    }
}
