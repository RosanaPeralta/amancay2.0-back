package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

import com.amancay.domain.model.Discount;

public record DiscountResponse(Long id, BigDecimal percentage, String description) {

    public static DiscountResponse from(Discount discount) {
        return discount == null ? null
                : new DiscountResponse(discount.getId(), discount.getPercentage(), discount.getDescription());
    }
}
