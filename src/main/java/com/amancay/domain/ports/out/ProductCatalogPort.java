package com.amancay.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductPage;
import com.amancay.domain.ports.in.ProductUseCases.ProductQuery;

public interface ProductCatalogPort {
    Product save(Product product);

    Optional<Product> findById(UUID id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    ProductPage search(ProductQuery query);

    void delete(Product product);
}