package com.amancay.domain.ports.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Category;

public interface CategoryUseCases {
    Category create(String name);

    Category update(UUID id, String name);

    void delete(UUID id);

    List<Category> list();
}