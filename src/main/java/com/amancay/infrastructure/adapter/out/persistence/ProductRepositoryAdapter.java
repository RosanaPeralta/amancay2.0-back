package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.ProductRepositoryPort;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;
import com.amancay.domain.model.ProductSummary;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.PageMapper;
import com.amancay.infrastructure.adapter.out.persistence.mapper.ProductPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.ProductSpecifications;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataDiscountRepository;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataProductRepository;

@Component
class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository repository;
    private final SpringDataDiscountRepository discountRepository;
    private final ProductPersistenceMapper mapper;

    ProductRepositoryAdapter(SpringDataProductRepository repository, SpringDataDiscountRepository discountRepository,
            ProductPersistenceMapper mapper) {
        this.repository = repository;
        this.discountRepository = discountRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public PageResult<ProductSummary> findSummaries(ProductFilter filter, ProductSort sort, PageQuery page) {
        return PageMapper.toPageResult(repository.findAll(
                ProductSpecifications.matching(filter.name(), filter.categoryId(), filter.active()),
                PageMapper.toPageable(page, toSort(sort))), mapper::toSummary);
    }

    @Override
    public Optional<ProductSummary> findSummaryById(UUID id) {
        return repository.findById(id).map(mapper::toSummary);
    }

    @Override
    public List<ProductSummary> findSummariesById(Collection<UUID> ids) {
        return repository.findAllById(ids).stream().map(mapper::toSummary).toList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlug(slug);
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, UUID id) {
        return repository.existsBySlugAndIdNot(slug, id);
    }

    @Override
    public boolean existsByDiscountId(Long discountId) {
        return repository.existsByDiscountId(discountId);
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = product.getId() == null ? new ProductJpaEntity()
                : repository.findById(product.getId()).orElseThrow(() -> new ProductNotFoundException(product.getId()));
        mapper.copyToEntity(product, entity);
        entity.setDiscount(product.getDiscount() == null ? null
                : discountRepository.getReferenceById(product.getDiscount().getId()));
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private static Sort toSort(ProductSort sort) {
        return switch (sort) {
            case NAME -> Sort.by("name").ascending();
            case NAME_DESC -> Sort.by("name").descending();
            case NEWEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name"));
        };
    }
}
