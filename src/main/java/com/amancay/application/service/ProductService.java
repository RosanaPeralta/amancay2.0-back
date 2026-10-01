package com.amancay.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.CreateDiscountUseCase;
import com.amancay.application.port.in.CreateProductCommand;
import com.amancay.application.port.in.CreateProductUseCase;
import com.amancay.application.port.in.DeleteProductUseCase;
import com.amancay.application.port.in.GetProductQuery;
import com.amancay.application.port.in.ListProductsQuery;
import com.amancay.application.port.in.ManageProductDiscountUseCase;
import com.amancay.application.port.in.UpdateProductCommand;
import com.amancay.application.port.in.UpdateProductUseCase;
import com.amancay.domain.exception.DiscountNotFoundException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.model.Discount;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;
import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.model.Slug;
import com.amancay.domain.port.DiscountRepositoryPort;
import com.amancay.domain.port.ProductRepositoryPort;

@Service
public class ProductService implements ListProductsQuery, GetProductQuery, CreateProductUseCase, UpdateProductUseCase,
        DeleteProductUseCase, ManageProductDiscountUseCase {

    private final ProductRepositoryPort productRepository;
    private final DiscountRepositoryPort discountRepository;
    private final CreateDiscountUseCase createDiscountUseCase;

    public ProductService(ProductRepositoryPort productRepository, DiscountRepositoryPort discountRepository,
            CreateDiscountUseCase createDiscountUseCase) {
        this.productRepository = productRepository;
        this.discountRepository = discountRepository;
        this.createDiscountUseCase = createDiscountUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductSummary> list(ProductFilter filter, ProductSort sort, PageQuery page) {
        return productRepository.findSummaries(filter, sort, page);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(UUID id) {
        return findProduct(id);
    }

    @Override
    @Transactional
    public Product create(CreateProductCommand command) {
        Product product = Product.create(command.name(), generateUniqueSlug(command.name(), null),
                command.shortDescription(), command.description(), command.active(), command.variants(),
                command.imageUrls(), command.categoryIds());
        if (command.discountId() != null) {
            product.assignDiscount(findDiscount(command.discountId()));
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product update(UUID id, UpdateProductCommand command) {
        Product product = findProduct(id);
        product.updateDetails(command.name(), generateUniqueSlug(command.name(), id), command.shortDescription(),
                command.description(), command.active(), command.categoryIds());
        product.replaceVariants(command.variants());
        product.replaceImages(command.images());
        if (command.discountId() != null) {
            product.assignDiscount(findDiscount(command.discountId()));
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        findProduct(id);
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Product assignDiscount(UUID productId, Long discountId) {
        Product product = findProduct(productId);
        product.assignDiscount(findDiscount(discountId));
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product createAndAssignDiscount(UUID productId, BigDecimal percentage, String description) {
        Discount discount = createDiscountUseCase.create(percentage, description);
        return assignDiscount(productId, discount.getId());
    }

    @Override
    @Transactional
    public Product removeDiscount(UUID productId) {
        Product product = findProduct(productId);
        product.removeDiscount();
        return productRepository.save(product);
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Discount findDiscount(Long id) {
        return discountRepository.findById(id).orElseThrow(() -> new DiscountNotFoundException(id));
    }

    private String generateUniqueSlug(String name, UUID currentId) {
        String base = Slug.from(name);
        String candidate = base;
        int suffix = 2;
        while (currentId == null ? productRepository.existsBySlug(candidate)
                : productRepository.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = Slug.withSuffix(base, suffix++);
        }
        return candidate;
    }
}
