package com.amancay.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.CreateDiscountUseCase;
import com.amancay.application.port.in.DeleteDiscountUseCase;
import com.amancay.application.port.in.ListDiscountsQuery;
import com.amancay.application.port.in.UpdateDiscountUseCase;
import com.amancay.application.port.out.DiscountRepositoryPort;
import com.amancay.application.port.out.ProductRepositoryPort;
import com.amancay.domain.exception.DiscountNotFoundException;
import com.amancay.domain.model.Discount;

@Service
public class DiscountService
        implements ListDiscountsQuery, CreateDiscountUseCase, UpdateDiscountUseCase, DeleteDiscountUseCase {

    private final DiscountRepositoryPort discountRepository;
    private final ProductRepositoryPort productRepository;
    private final AdminGuard adminGuard;

    public DiscountService(DiscountRepositoryPort discountRepository, ProductRepositoryPort productRepository,
            AdminGuard adminGuard) {
        this.discountRepository = discountRepository;
        this.productRepository = productRepository;
        this.adminGuard = adminGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Discount> listAll() {
        return discountRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Discount getById(Long id) {
        return discountRepository.findById(id).orElseThrow(() -> new DiscountNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Discount> searchByDescription(String description) {
        if (description == null || description.isBlank()) {
            return listAll();
        }
        return discountRepository.findByDescription(description);
    }

    @Override
    @Transactional
    public Discount create(UUID requesterId, BigDecimal percentage, String description) {
        adminGuard.requireAdmin(requesterId);
        return discountRepository.save(Discount.create(percentage, description));
    }

    @Override
    @Transactional
    public Discount update(UUID requesterId, Long id, BigDecimal percentage, String description) {
        adminGuard.requireAdmin(requesterId);
        Discount discount = getById(id);
        discount.update(percentage, description);
        return discountRepository.save(discount);
    }

    @Override
    @Transactional
    public void delete(UUID requesterId, Long id) {
        adminGuard.requireAdmin(requesterId);
        getById(id);
        if (productRepository.existsByDiscountId(id)) {
            throw new IllegalStateException("Cannot delete discount because it is associated with products");
        }
        discountRepository.deleteById(id);
    }
}
