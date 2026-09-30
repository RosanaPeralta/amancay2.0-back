package com.amancay.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Discount(Long id, BigDecimal percentage, String description, boolean assignedToProducts) {
    public BigDecimal applyTo(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (percentage == null) {
            return amount;
        }
        validatePercentage(percentage);
        BigDecimal percentValue = percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return amount.multiply(BigDecimal.ONE.subtract(percentValue));
    }

    public static void validatePercentage(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
    }
}