package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Category;

public interface UpdateCategoryUseCase {
    Category rename(UUID requesterId, UUID id, String name);
}
