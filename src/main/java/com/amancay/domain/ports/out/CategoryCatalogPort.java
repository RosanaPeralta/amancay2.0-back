package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Category;

public interface CategoryCatalogPort {
    List<Category> findAll();

    Optional<Category> findById(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Category save(Category category);

    void deleteById(UUID id);
}