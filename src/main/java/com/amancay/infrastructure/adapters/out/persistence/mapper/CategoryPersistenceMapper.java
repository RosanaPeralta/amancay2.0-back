package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.Category;

public final class CategoryPersistenceMapper {
    private CategoryPersistenceMapper() {
    }

    public static Category toDomain(com.amancay.entity.Category entity) {
        return new Category(entity.getId(), entity.getName());
    }

    public static com.amancay.entity.Category toPersistence(Category category,
            com.amancay.entity.Category entity) {
        entity.setName(category.name());
        return entity;
    }
}