package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Category;
import com.amancay.domain.ports.in.CategoryUseCases;
import com.amancay.domain.ports.out.CategoryCatalogPort;
import com.amancay.exceptions.CategoryNotFoundException;
import com.amancay.exceptions.DuplicateCategoryNameException;

public class CategoryApplicationService implements CategoryUseCases {
    private final CategoryCatalogPort categoryCatalog;

    public CategoryApplicationService(CategoryCatalogPort categoryCatalog) {
        this.categoryCatalog = categoryCatalog;
    }

    @Override
    public Category create(String name) {
        if (categoryCatalog.existsByName(name)) {
            throw new DuplicateCategoryNameException(name);
        }
        return categoryCatalog.save(new Category(null, name));
    }

    @Override
    public Category update(UUID id, String name) {
        findCategory(id);
        if (categoryCatalog.existsByNameAndIdNot(name, id)) {
            throw new DuplicateCategoryNameException(name);
        }
        return categoryCatalog.save(new Category(id, name));
    }

    @Override
    public void delete(UUID id) {
        findCategory(id);
        categoryCatalog.deleteById(id);
    }

    @Override
    public List<Category> list() {
        return categoryCatalog.findAll();
    }

    private Category findCategory(UUID id) {
        return categoryCatalog.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }
}