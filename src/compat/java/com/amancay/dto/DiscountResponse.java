package com.amancay.dto;

import java.math.BigDecimal;

public record DiscountResponse(Long id, BigDecimal percentage, String description) {
}