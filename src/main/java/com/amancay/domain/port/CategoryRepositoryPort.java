package com.amancay.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Category;

public interface CategoryRepositoryPort {
    List<Category> findAll();

    Optional<Category> findById(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Category save(Category category);

    void deleteById(UUID id);
}
