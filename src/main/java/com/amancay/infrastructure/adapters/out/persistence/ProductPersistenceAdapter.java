package com.amancay.infrastructure.adapters.out.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.amancay.domain.model.ProductPage;
import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.ports.in.ProductUseCases.ProductQuery;
import com.amancay.domain.ports.out.DiscountLookupPort;
import com.amancay.domain.ports.out.ProductCatalogPort;
import com.amancay.infrastructure.adapters.out.persistence.repository.DiscountRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.ProductRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.ProductSpecifications;
import com.amancay.infrastructure.adapters.out.persistence.mapper.ProductPersistenceMapper;

@Repository
public class ProductPersistenceAdapter implements ProductCatalogPort, DiscountLookupPort {
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;

    public ProductPersistenceAdapter(ProductRepository productRepository, DiscountRepository discountRepository) {
        this.productRepository = productRepository;
        this.discountRepository = discountRepository;
    }

    @Override
    public com.amancay.domain.model.Product save(com.amancay.domain.model.Product product) {
        com.amancay.entity.Product entity = product.id() == null ? new com.amancay.entity.Product()
                : productRepository.findById(product.id()).orElseGet(com.amancay.entity.Product::new);
        com.amancay.entity.Discount discount = product.discount() == null ? null
            : discountRepository.findById(product.discount().id()).orElseThrow();
        ProductPersistenceMapper.toPersistence(product, entity, discount);
        return ProductPersistenceMapper.toDomain(productRepository.save(entity));
    }

    @Override
    public Optional<com.amancay.domain.model.Product> findById(UUID id) {
        return productRepository.findById(id).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return productRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, UUID id) {
        return productRepository.existsBySlugAndIdNot(slug, id);
    }

    @Override
    public ProductPage search(ProductQuery query) {
        List<Sort.Order> sortOrders = query.sort().stream()
                .map(order -> order.ascending() ? Sort.Order.asc(order.property()) : Sort.Order.desc(order.property()))
                .toList();
        PageRequest pageRequest = PageRequest.of(query.page(), query.size(), Sort.by(sortOrders));
        Page<com.amancay.entity.Product> page = productRepository.findAll(
                ProductSpecifications.matching(query.name(), query.categoryId(), query.active()), pageRequest);
        List<ProductSummary> summaries = page.getContent().stream()
                .map(product -> new ProductSummary(product.getId(), product.getName(), product.getSlug(),
                        product.isActive(), product.getCreatedAt(), product.getUpdatedAt()))
                .toList();
        return new ProductPage(summaries, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public void delete(com.amancay.domain.model.Product product) {
        productRepository.deleteById(product.id());
    }

    @Override
    public Optional<com.amancay.domain.model.Product.Discount> findDiscountById(Long id) {
        if (discountRepository == null) {
            return Optional.empty();
        }
        return discountRepository.findById(id).map(discount -> new com.amancay.domain.model.Product.Discount(
                discount.getId(), discount.getPercentage(), discount.getDescription()));
    }

}