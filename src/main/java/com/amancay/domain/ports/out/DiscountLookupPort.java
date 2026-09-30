package com.amancay.domain.ports.out;

import java.util.Optional;

import com.amancay.domain.model.Product.Discount;

public interface DiscountLookupPort {
    Optional<Discount> findDiscountById(Long id);
}