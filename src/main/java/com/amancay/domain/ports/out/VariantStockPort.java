package com.amancay.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.VariantStock;

public interface VariantStockPort {
    Optional<VariantStock> findVariantById(UUID id);
}