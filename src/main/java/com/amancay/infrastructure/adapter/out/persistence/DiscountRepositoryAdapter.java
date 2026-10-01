package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Discount;
import com.amancay.domain.port.DiscountRepositoryPort;
import com.amancay.infrastructure.adapter.out.persistence.entity.DiscountJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.DiscountPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataDiscountRepository;

@Component
class DiscountRepositoryAdapter implements DiscountRepositoryPort {

    private final SpringDataDiscountRepository repository;
    private final DiscountPersistenceMapper mapper;

    DiscountRepositoryAdapter(SpringDataDiscountRepository repository, DiscountPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Discount> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Discount> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Discount> findByDescription(String description) {
        return repository.findByDescription(description).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Discount save(Discount discount) {
        DiscountJpaEntity entity = discount.getId() == null ? new DiscountJpaEntity()
                : repository.findById(discount.getId()).orElseGet(DiscountJpaEntity::new);
        mapper.copyToEntity(discount, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
