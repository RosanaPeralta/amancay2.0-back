package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Category;
import com.amancay.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;

@Component
public class CategoryPersistenceMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        return new Category(entity.getId(), entity.getName());
    }

    public void copyToEntity(Category category, CategoryJpaEntity entity) {
        entity.setName(category.getName());
    }
}
