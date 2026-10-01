package com.amancay.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;
import com.amancay.domain.model.ProductSummary;

public interface ProductRepositoryPort {
    Optional<Product> findById(UUID id);

    boolean existsById(UUID id);

    PageResult<ProductSummary> findSummaries(ProductFilter filter, ProductSort sort, PageQuery page);

    Optional<ProductSummary> findSummaryById(UUID id);

    List<ProductSummary> findSummariesById(Collection<UUID> ids);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByDiscountId(Long discountId);

    // Devuelve el producto tal como quedo persistido (con ids y timestamps asignados).
    Product save(Product product);

    void deleteById(UUID id);
}
