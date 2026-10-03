package com.amancay.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

import com.amancay.domain.model.Discount;

public interface UpdateDiscountUseCase {
    Discount update(UUID requesterId, Long id, BigDecimal percentage, String description);
}
