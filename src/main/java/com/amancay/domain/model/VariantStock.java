package com.amancay.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantStock(UUID id, BigDecimal price, int quantityAvailable) {
}