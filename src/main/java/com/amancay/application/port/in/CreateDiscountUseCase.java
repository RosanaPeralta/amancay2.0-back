package com.amancay.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

import com.amancay.domain.model.Discount;

public interface CreateDiscountUseCase {
    Discount create(UUID requesterId, BigDecimal percentage, String description);
}
