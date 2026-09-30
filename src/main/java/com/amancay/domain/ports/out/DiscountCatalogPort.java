package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;

import com.amancay.domain.model.Discount;

public interface DiscountCatalogPort {
    List<Discount> findAll();

    Optional<Discount> findById(Long id);

    List<Discount> findByDescription(String description);

    Discount save(Discount discount);

    boolean hasProducts(Long discountId);

    void deleteById(Long id);
}