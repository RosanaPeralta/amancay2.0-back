package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Category;

public interface CreateCategoryUseCase {
    Category create(UUID requesterId, String name);
}
