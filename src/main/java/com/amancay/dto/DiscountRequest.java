package com.amancay.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiscountRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") 
        BigDecimal percentage,
        @NotBlank 
        String description) {
}
