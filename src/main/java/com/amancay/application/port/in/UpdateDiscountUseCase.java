package com.amancay.application.port.in;

import java.math.BigDecimal;

import com.amancay.domain.model.Discount;

public interface UpdateDiscountUseCase {
    Discount update(Long id, BigDecimal percentage, String description);
}
