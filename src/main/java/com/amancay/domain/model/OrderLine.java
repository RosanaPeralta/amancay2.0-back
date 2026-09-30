package com.amancay.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLine(UUID id, UUID productVariantId, int quantity, BigDecimal unitPrice) {
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}