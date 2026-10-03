package com.amancay.application.service.fake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.amancay.application.port.out.ProductRepositoryPort;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductImage;
import com.amancay.domain.model.ProductSort;
import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.model.ProductVariant;

// Imita a la base: asigna ids a producto, variantes e imagenes y timestamps crecientes.
public class InMemoryProductRepository implements ProductRepositoryPort {

    private final Map<UUID, Product> store = new LinkedHashMap<>();
    public final List<String> calls = new ArrayList<>();
    private Instant clock = Instant.parse("2026-01-01T00:00:00Z");

    public Product add(String name, String slug, boolean active, Set<UUID> categoryIds) {
        return save(new Product(null, name, slug, null, null, active, null, null, List.of(), List.of(), categoryIds,
                null));
    }

    public long count(String call) {
        return calls.stream().filter(call::equals).count();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        calls.add("findById");
        return Optional.ofNullable(store.get(id)).map(InMemoryProductRepository::copy);
    }

    @Override
    public boolean existsById(UUID id) {
        return store.containsKey(id);
    }

    @Override
    public PageResult<ProductSummary> findSummaries(ProductFilter filter, ProductSort sort, PageQuery page) {
        Comparator<ProductSummary> order = switch (sort) {
            case NAME -> Comparator.comparing(ProductSummary::name);
            case NAME_DESC -> Comparator.comparing(ProductSummary::name).reversed();
            case NEWEST -> Comparator.comparing(ProductSummary::createdAt).reversed()
                    .thenComparing(ProductSummary::name);
        };
        return Pages.of(store.values().stream()
                .filter(p -> filter.name() == null
                        || p.getName().toLowerCase(Locale.ROOT).contains(filter.name().toLowerCase(Locale.ROOT)))
                .filter(p -> filter.categoryId() == null || p.getCategoryIds().contains(filter.categoryId()))
                .filter(p -> filter.active() == null || p.isActive() == filter.active())
                .map(Product::toSummary)
                .sorted(order)
                .toList(), page);
    }

    @Override
    public Optional<ProductSummary> findSummaryById(UUID id) {
        calls.add("findSummaryById");
        return Optional.ofNullable(store.get(id)).map(Product::toSummary);
    }

    @Override
    public List<ProductSummary> findSummariesById(Collection<UUID> ids) {
        calls.add("findSummariesById");
        return ids.stream().map(store::get).filter(p -> p != null).map(Product::toSummary).toList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return store.values().stream().anyMatch(p -> p.getSlug().equals(slug));
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, UUID id) {
        return store.values().stream().anyMatch(p -> p.getSlug().equals(slug) && !p.getId().equals(id));
    }

    @Override
    public boolean existsByDiscountId(Long discountId) {
        return store.values().stream()
                .anyMatch(p -> p.getDiscount() != null && p.getDiscount().getId().equals(discountId));
    }

    @Override
    public Product save(Product product) {
        calls.add("save");
        clock = clock.plusSeconds(1);
        UUID id = product.getId() == null ? UUID.randomUUID() : product.getId();
        Product saved = new Product(id, product.getName(), product.getSlug(), product.getShortDescription(),
                product.getDescription(), product.isActive(),
                product.getCreatedAt() == null ? clock : product.getCreatedAt(), clock,
                product.getVariants().stream()
                        .map(v -> new ProductVariant(v.getId() == null ? UUID.randomUUID() : v.getId(), v.getPrice(),
                                v.getStockQuantity()))
                        .toList(),
                product.getImages().stream()
                        .map(i -> new ProductImage(i.getId() == null ? UUID.randomUUID() : i.getId(), i.getImageUrl()))
                        .toList(),
                product.getCategoryIds(), product.getDiscount());
        store.put(id, saved);
        return copy(saved);
    }

    @Override
    public void deleteById(UUID id) {
        store.remove(id);
    }

    private static Product copy(Product p) {
        return new Product(p.getId(), p.getName(), p.getSlug(), p.getShortDescription(), p.getDescription(),
                p.isActive(), p.getCreatedAt(), p.getUpdatedAt(),
                p.getVariants().stream().map(v -> new ProductVariant(v.getId(), v.getPrice(), v.getStockQuantity()))
                        .toList(),
                p.getImages().stream().map(i -> new ProductImage(i.getId(), i.getImageUrl())).toList(),
                p.getCategoryIds(), p.getDiscount());
    }
}
