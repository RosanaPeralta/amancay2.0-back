package com.amancay.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantDto(UUID id, BigDecimal price, int stockQuantity) {
}
