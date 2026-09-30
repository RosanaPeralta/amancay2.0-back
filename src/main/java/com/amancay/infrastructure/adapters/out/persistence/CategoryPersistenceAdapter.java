package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.Category;
import com.amancay.domain.ports.out.CategoryCatalogPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.CategoryPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.CategoryRepository;

@Repository
public class CategoryPersistenceAdapter implements CategoryCatalogPort {
    private final CategoryRepository repository;

    public CategoryPersistenceAdapter(CategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return repository.findAll().stream().map(CategoryPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(UUID id) {
        return repository.findById(id).map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return repository.existsByNameAndIdNot(name, id);
    }

    @Override
    @Transactional
    public Category save(Category category) {
        com.amancay.entity.Category entity = category.id() == null ? new com.amancay.entity.Category()
                : repository.findById(category.id()).orElseGet(com.amancay.entity.Category::new);
        return CategoryPersistenceMapper.toDomain(repository.save(CategoryPersistenceMapper.toPersistence(category, entity)));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}