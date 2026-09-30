package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.Discount;
import com.amancay.domain.ports.out.DiscountCatalogPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.DiscountPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.DiscountRepository;

@Repository
public class DiscountPersistenceAdapter implements DiscountCatalogPort {
    private final DiscountRepository repository;

    public DiscountPersistenceAdapter(DiscountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Discount> findAll() {
        return repository.findAll().stream().map(DiscountPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Discount> findById(Long id) {
        return repository.findById(id).map(DiscountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Discount> findByDescription(String description) {
        return repository.findByDescription(description).stream().map(DiscountPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Discount save(Discount discount) {
        com.amancay.entity.Discount entity = discount.id() == null ? new com.amancay.entity.Discount()
                : repository.findById(discount.id()).orElseGet(com.amancay.entity.Discount::new);
        return DiscountPersistenceMapper.toDomain(repository.save(DiscountPersistenceMapper.toPersistence(discount, entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProducts(Long discountId) {
        return repository.findById(discountId).map(discount -> !discount.getProducts().isEmpty()).orElse(false);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}