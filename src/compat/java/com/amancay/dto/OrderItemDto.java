package com.amancay.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(UUID id, UUID productVariantId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
