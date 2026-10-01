package com.amancay.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Getter;

@Getter
public class Discount {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final Long id;
    private BigDecimal percentage;
    private String description;

    public Discount(Long id, BigDecimal percentage, String description) {
        this.id = id;
        this.percentage = percentage;
        this.description = description;
    }

    public static Discount create(BigDecimal percentage, String description) {
        validate(percentage, description);
        return new Discount(null, percentage, description);
    }

    public void update(BigDecimal percentage, String description) {
        validate(percentage, description);
        this.percentage = percentage;
        this.description = description;
    }

    // Precio final de amount con este descuento aplicado.
    public BigDecimal applyTo(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (percentage == null) {
            return amount;
        }
        validatePercentage(percentage);
        BigDecimal percentValue = percentage.divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return amount.multiply(BigDecimal.ONE.subtract(percentValue));
    }

    private static void validate(BigDecimal percentage, String description) {
        if (percentage == null) {
            throw new IllegalArgumentException("percentage is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        validatePercentage(percentage);
    }

    private static void validatePercentage(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
    }
}
