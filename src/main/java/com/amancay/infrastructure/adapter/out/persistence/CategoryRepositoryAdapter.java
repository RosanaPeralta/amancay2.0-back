package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.CategoryRepositoryPort;
import com.amancay.domain.model.Category;
import com.amancay.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.CategoryPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataCategoryRepository;

@Component
class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final SpringDataCategoryRepository repository;
    private final CategoryPersistenceMapper mapper;

    CategoryRepositoryAdapter(SpringDataCategoryRepository repository, CategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Category> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return repository.existsByNameAndIdNot(name, id);
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = category.getId() == null ? new CategoryJpaEntity()
                : repository.findById(category.getId()).orElseGet(CategoryJpaEntity::new);
        mapper.copyToEntity(category, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
