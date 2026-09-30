package com.amancay.application.service;

import java.math.BigDecimal;
import java.util.List;

import com.amancay.domain.model.Discount;
import com.amancay.domain.ports.in.DiscountUseCases;
import com.amancay.domain.ports.out.DiscountCatalogPort;
import com.amancay.exceptions.DiscountNotFoundException;

public class DiscountApplicationService implements DiscountUseCases {
    private final DiscountCatalogPort discountCatalog;

    public DiscountApplicationService(DiscountCatalogPort discountCatalog) {
        this.discountCatalog = discountCatalog;
    }

    @Override
    public List<Discount> list() {
        return discountCatalog.findAll();
    }

    @Override
    public Discount getById(Long id) {
        return findDiscount(id);
    }

    @Override
    public Discount create(BigDecimal percentage, String description) {
        validateRequest(percentage, description);
        return discountCatalog.save(new Discount(null, percentage, description, false));
    }

    @Override
    public Discount update(Long id, BigDecimal percentage, String description) {
        Discount current = findDiscount(id);
        validateRequest(percentage, description);
        BigDecimal updatedPercentage = percentage == null ? current.percentage() : percentage;
        String updatedDescription = description == null || description.isBlank() ? current.description() : description;
        return discountCatalog.save(new Discount(id, updatedPercentage, updatedDescription, current.assignedToProducts()));
    }

    @Override
    public void delete(Long id) {
        Discount discount = findDiscount(id);
        if (discount.assignedToProducts()) {
            throw new IllegalStateException("Cannot delete discount because it is associated with products");
        }
        discountCatalog.deleteById(id);
    }

    @Override
    public List<Discount> searchByDescription(String description) {
        return description == null || description.isBlank() ? list() : discountCatalog.findByDescription(description);
    }

    @Override
    public BigDecimal applyDiscount(BigDecimal amount, Discount discount) {
        return discount == null ? amount : discount.applyTo(amount);
    }

    private Discount findDiscount(Long id) {
        return discountCatalog.findById(id).orElseThrow(() -> new DiscountNotFoundException(id));
    }

    private void validateRequest(BigDecimal percentage, String description) {
        if (percentage == null) {
            throw new IllegalArgumentException("percentage is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        Discount.validatePercentage(percentage);
    }
}