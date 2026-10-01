package com.amancay.domain.port;

import java.util.List;
import java.util.Optional;

import com.amancay.domain.model.Discount;

public interface DiscountRepositoryPort {
    List<Discount> findAll();

    Optional<Discount> findById(Long id);

    List<Discount> findByDescription(String description);

    Discount save(Discount discount);

    void deleteById(Long id);
}
