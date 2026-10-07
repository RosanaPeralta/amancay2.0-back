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
import com.amancay.application.port.out.DiscountRepositoryPort;
import com.amancay.application.port.out.ProductRepositoryPort;
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

@Service
public class ProductService implements ListProductsQuery, GetProductQuery, CreateProductUseCase, UpdateProductUseCase,
        DeleteProductUseCase, ManageProductDiscountUseCase {

    private final ProductRepositoryPort productRepository;
    private final DiscountRepositoryPort discountRepository;
    private final CreateDiscountUseCase createDiscountUseCase;
    private final AdminGuard adminGuard;

    public ProductService(ProductRepositoryPort productRepository, DiscountRepositoryPort discountRepository,
            CreateDiscountUseCase createDiscountUseCase, AdminGuard adminGuard) {
        this.productRepository = productRepository;
        this.discountRepository = discountRepository;
        this.createDiscountUseCase = createDiscountUseCase;
        this.adminGuard = adminGuard;
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
    public Product create(UUID requesterId, CreateProductCommand command) {
        adminGuard.requireAdmin(requesterId);
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
    public Product update(UUID requesterId, UUID id, UpdateProductCommand command) {
        adminGuard.requireAdmin(requesterId);
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
    public void delete(UUID requesterId, UUID id) {
        adminGuard.requireAdmin(requesterId);
        findProduct(id);
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Product assignDiscount(UUID requesterId, UUID productId, Long discountId) {
        adminGuard.requireAdmin(requesterId);
        return assign(productId, discountId);
    }

    @Override
    @Transactional
    public Product createAndAssignDiscount(UUID requesterId, UUID productId, BigDecimal percentage,
            String description) {
        Discount discount = createDiscountUseCase.create(requesterId, percentage, description);
        return assign(productId, discount.getId());
    }

    @Override
    @Transactional
    public Product deleteDiscount(UUID requesterId, UUID productId) {
        adminGuard.requireAdmin(requesterId);
        Product product = findProduct(productId);
        product.deleteDiscount();
        return productRepository.save(product);
    }

    private Product assign(UUID productId, Long discountId) {
        Product product = findProduct(productId);
        product.assignDiscount(findDiscount(discountId));
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
