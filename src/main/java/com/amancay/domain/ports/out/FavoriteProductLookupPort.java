package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.amancay.domain.model.ProductSummary;

public interface FavoriteProductLookupPort {
    Optional<ProductSummary> findById(UUID productId);

    List<ProductSummary> findAll(Set<UUID> productIds);
}