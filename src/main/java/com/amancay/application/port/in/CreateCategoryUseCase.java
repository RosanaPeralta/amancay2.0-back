package com.amancay.application.port.in;

import com.amancay.domain.model.Category;

public interface CreateCategoryUseCase {
    Category create(String name);
}
