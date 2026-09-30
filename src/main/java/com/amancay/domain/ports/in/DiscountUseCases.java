package com.amancay.domain.ports.in;

import java.math.BigDecimal;
import java.util.List;

import com.amancay.domain.model.Discount;

public interface DiscountUseCases {
    List<Discount> list();

    Discount getById(Long id);

    Discount create(BigDecimal percentage, String description);

    Discount update(Long id, BigDecimal percentage, String description);

    void delete(Long id);

    List<Discount> searchByDescription(String description);

    BigDecimal applyDiscount(BigDecimal amount, Discount discount);
}