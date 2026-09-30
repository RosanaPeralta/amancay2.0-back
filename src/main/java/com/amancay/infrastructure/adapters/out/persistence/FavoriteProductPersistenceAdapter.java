package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.ports.out.FavoriteProductLookupPort;
import com.amancay.infrastructure.adapters.out.persistence.repository.ProductRepository;

@Repository
public class FavoriteProductPersistenceAdapter implements FavoriteProductLookupPort {
    private final ProductRepository repository;

    public FavoriteProductPersistenceAdapter(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSummary> findById(UUID productId) {
        return repository.findById(productId).map(product -> new ProductSummary(product.getId(), product.getName(),
                product.getSlug(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummary> findAll(Set<UUID> productIds) {
        return repository.findAllById(productIds).stream()
                .map(product -> new ProductSummary(product.getId(), product.getName(), product.getSlug(),
                        product.isActive(), product.getCreatedAt(), product.getUpdatedAt()))
                .toList();
    }
}