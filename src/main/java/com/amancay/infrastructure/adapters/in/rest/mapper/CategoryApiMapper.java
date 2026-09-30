package com.amancay.infrastructure.adapters.in.rest.mapper;

import com.amancay.domain.model.Category;
import com.amancay.dto.CategoryDto;

public final class CategoryApiMapper {
    private CategoryApiMapper() {
    }

    public static CategoryDto toDto(Category category) {
        return new CategoryDto(category.id(), category.name());
    }
}