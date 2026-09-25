package com.amancay.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.DiscountRequest;
import com.amancay.entity.Discount;
import com.amancay.exceptions.DiscountNotFoundException;
import com.amancay.repository.DiscountRepository;

@Service
public class DiscountService {
    private final DiscountRepository discountRepository;

    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @Transactional(readOnly = true)
    public List<Discount> list() {
        return discountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Discount getById(Long id) {
        return discountRepository.findById(id).orElseThrow(() -> new DiscountNotFoundException(id));
    }

    @Transactional
    public Discount create(DiscountRequest request) {
        validateRequest(request);
        Discount discount = new Discount();
        discount.setPercentage(request.percentage());
        discount.setDescription(request.description());
        return discountRepository.save(discount);
    }

    @Transactional
    public Discount update(Long id, DiscountRequest request) {
        Discount discount = getById(id);
        validateRequest(request);

        if (request.percentage() != null) {
            discount.setPercentage(request.percentage());
        }
        if (request.description() != null && !request.description().isBlank()) {
            discount.setDescription(request.description());
        }
        return discountRepository.save(discount);
    }

    @Transactional
    public void delete(Long id) {
        Discount discount = getById(id);
        if (!discount.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete discount because it is associated with products");
        }
        discountRepository.delete(discount);
    }

    @Transactional(readOnly = true)
    public List<Discount> searchByDescription(String description) {
        if (description == null || description.isBlank()) {
            return list();
        }
        return discountRepository.findByDescription(description);
    }

    public BigDecimal applyDiscount(BigDecimal amount, Discount discount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (discount == null || discount.getPercentage() == null) {
            return amount;
        }
        validatePercentage(discount.getPercentage());
        BigDecimal percentValue = discount.getPercentage().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        return amount.multiply(BigDecimal.ONE.subtract(percentValue));
    }

    private void validateRequest(DiscountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("discount request is required");
        }
        if (request.percentage() == null) {
            throw new IllegalArgumentException("percentage is required");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        validatePercentage(request.percentage());
    }

    private void validatePercentage(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
    }
}