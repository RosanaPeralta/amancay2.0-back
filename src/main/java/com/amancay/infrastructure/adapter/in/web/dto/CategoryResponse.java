package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.UUID;

import com.amancay.domain.model.Category;

public record CategoryResponse(
        UUID id,
        String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
