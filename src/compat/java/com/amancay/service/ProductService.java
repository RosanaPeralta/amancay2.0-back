package com.amancay.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.ProductApplicationService;
import com.amancay.domain.model.ProductPage;
import com.amancay.domain.ports.in.ProductUseCases;
import com.amancay.domain.ports.in.ProductUseCases.CreateProduct;
import com.amancay.domain.ports.in.ProductUseCases.ImageUpdate;
import com.amancay.domain.ports.in.ProductUseCases.ProductQuery;
import com.amancay.domain.ports.in.ProductUseCases.SortOrder;
import com.amancay.domain.ports.in.ProductUseCases.UpdateProduct;
import com.amancay.domain.ports.in.ProductUseCases.VariantInput;
import com.amancay.domain.ports.in.ProductUseCases.VariantUpdate;
import com.amancay.domain.ports.out.DiscountLookupPort;
import com.amancay.domain.ports.out.ProductCatalogPort;
import com.amancay.dto.CreateProductRequest;
import com.amancay.dto.PageResponse;
import com.amancay.dto.ProductDto;
import com.amancay.dto.ProductSummaryDto;
import com.amancay.dto.UpdateProductRequest;
import com.amancay.infrastructure.adapters.in.rest.mapper.ProductApiMapper;
import com.amancay.infrastructure.adapters.out.persistence.ProductPersistenceAdapter;
import com.amancay.repository.DiscountRepository;
import com.amancay.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductUseCases productUseCases;
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductUseCases productUseCases, ProductRepository productRepository) {
        this.productUseCases = productUseCases;
        this.productRepository = productRepository;
    }

    public ProductService(ProductRepository productRepository, DiscountRepository discountRepository) {
        this.productRepository = productRepository;
        ProductPersistenceAdapter persistenceAdapter = new ProductPersistenceAdapter(productRepository, discountRepository);
        this.productUseCases = new ProductApplicationService(persistenceAdapter, (DiscountLookupPort) persistenceAdapter);
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        CreateProduct command = new CreateProduct(request.name(), request.shortDescription(), request.description(),
                request.active(), request.variants() == null ? null : request.variants().stream()
                        .map(variant -> new VariantInput(variant.price(), variant.stockQuantity())).toList(),
                request.imageUrls(), request.categoryIds() == null ? null : java.util.Set.copyOf(request.categoryIds()),
                request.discountId());
        return ProductApiMapper.toDto(productUseCases.create(command));
    }

    @Transactional
    public ProductDto updateProduct(UUID id, UpdateProductRequest request) {
        UpdateProduct command = new UpdateProduct(request.name(), request.shortDescription(), request.description(),
                request.active(), request.variants() == null ? null : request.variants().stream()
                        .map(variant -> new VariantUpdate(variant.id(), variant.price(), variant.stockQuantity())).toList(),
                request.images() == null ? null : request.images().stream()
                        .map(image -> new ImageUpdate(image.id(), image.imageUrl())).toList(),
                request.categoryIds() == null ? null : java.util.Set.copyOf(request.categoryIds()), request.discountId());
        return ProductApiMapper.toDto(productUseCases.update(id, command));
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID id) {
        return ProductApiMapper.toDto(productUseCases.getById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> listProducts(Pageable pageable, Optional<String> name,
            Optional<UUID> categoryId, Optional<Boolean> active) {
        ProductQuery query = new ProductQuery(pageable.getPageNumber(), pageable.getPageSize(), name.orElse(null),
                categoryId.orElse(null), active.orElse(null), pageable.getSort().stream()
                        .map(order -> new SortOrder(order.getProperty(), order.isAscending())).toList());
        ProductPage page = productUseCases.list(query);
        return new PageResponse<>(page.content().stream().map(ProductApiMapper::toSummaryDto).toList(), page.page(),
                page.size(), page.totalElements(), page.totalPages());
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productUseCases.delete(id);
    }

    @Transactional
    public com.amancay.entity.Product assignDiscount(UUID productId, Long discountId) {
        productUseCases.assignDiscount(productId, discountId);
        return productRepository.findById(productId).orElseThrow();
    }

    @Transactional
    public com.amancay.entity.Product removeDiscount(UUID productId) {
        productUseCases.removeDiscount(productId);
        return productRepository.findById(productId).orElseThrow();
    }
}
