package com.amancay.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.DiscountApplicationService;
import com.amancay.domain.model.Discount;
import com.amancay.domain.ports.in.DiscountUseCases;
import com.amancay.domain.ports.out.DiscountCatalogPort;
import com.amancay.dto.DiscountRequest;
import com.amancay.infrastructure.adapters.out.persistence.DiscountPersistenceAdapter;
import com.amancay.repository.DiscountRepository;

@Service
public class DiscountService {
    private final DiscountUseCases discountUseCases;
    private final DiscountRepository discountRepository;

    @Autowired
    public DiscountService(DiscountUseCases discountUseCases, DiscountRepository discountRepository) {
        this.discountUseCases = discountUseCases;
        this.discountRepository = discountRepository;
    }

    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
        DiscountCatalogPort adapter = new DiscountPersistenceAdapter(discountRepository);
        this.discountUseCases = new DiscountApplicationService(adapter);
    }

    @Transactional(readOnly = true)
    public List<com.amancay.entity.Discount> list() {
        return discountUseCases.list().stream().map(this::toPersistenceModel).toList();
    }

    @Transactional(readOnly = true)
    public com.amancay.entity.Discount getById(Long id) {
        return toPersistenceModel(discountUseCases.getById(id));
    }

    @Transactional
    public com.amancay.entity.Discount create(DiscountRequest request) {
        return toPersistenceModel(discountUseCases.create(request.percentage(), request.description()));
    }

    @Transactional
    public com.amancay.entity.Discount update(Long id, DiscountRequest request) {
        return toPersistenceModel(discountUseCases.update(id, request.percentage(), request.description()));
    }

    @Transactional
    public void delete(Long id) {
        discountUseCases.delete(id);
    }

    @Transactional(readOnly = true)
    public List<com.amancay.entity.Discount> searchByDescription(String description) {
        return discountUseCases.searchByDescription(description).stream().map(this::toPersistenceModel).toList();
    }

    public BigDecimal applyDiscount(BigDecimal amount, com.amancay.entity.Discount discount) {
        Discount model = discount == null ? null : new Discount(discount.getId(), discount.getPercentage(),
                discount.getDescription(), discount.getProducts() != null && !discount.getProducts().isEmpty());
        return discountUseCases.applyDiscount(amount, model);
    }

    private com.amancay.entity.Discount toPersistenceModel(Discount model) {
        com.amancay.entity.Discount discount = model.id() == null ? new com.amancay.entity.Discount()
                : discountRepository.findById(model.id()).orElseGet(com.amancay.entity.Discount::new);
        discount.setId(model.id());
        discount.setPercentage(model.percentage());
        discount.setDescription(model.description());
        return discount;
    }
}