package com.amancay.application.port.in;

import java.math.BigDecimal;

import com.amancay.domain.model.Discount;

public interface CreateDiscountUseCase {
    Discount create(BigDecimal percentage, String description);
}
