package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amancay.application.service.CategoryApplicationService;
import com.amancay.domain.model.Category;
import com.amancay.domain.ports.in.CategoryUseCases;
import com.amancay.domain.ports.out.CategoryCatalogPort;
import com.amancay.dto.CategoryDto;
import com.amancay.dto.CreateCategoryRequest;
import com.amancay.dto.UpdateCategoryRequest;
import com.amancay.infrastructure.adapters.in.rest.mapper.CategoryApiMapper;
import com.amancay.infrastructure.adapters.out.persistence.CategoryPersistenceAdapter;
import com.amancay.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryUseCases categoryUseCases;

    @Autowired
    public CategoryService(CategoryUseCases categoryUseCases) {
        this.categoryUseCases = categoryUseCases;
    }

    public CategoryService(CategoryRepository categoryRepository) {
        CategoryCatalogPort adapter = new CategoryPersistenceAdapter(categoryRepository);
        this.categoryUseCases = new CategoryApplicationService(adapter);
    }

    public CategoryDto createCategory(CreateCategoryRequest request) {
        return CategoryApiMapper.toDto(categoryUseCases.create(request.name()));
    }

    public CategoryDto updateCategory(UUID id, UpdateCategoryRequest request) {
        return CategoryApiMapper.toDto(categoryUseCases.update(id, request.name()));
    }

    public void deleteCategory(UUID id) {
        categoryUseCases.delete(id);
    }

    public List<CategoryDto> getAllCategories() {
        return categoryUseCases.list().stream().map(CategoryApiMapper::toDto).toList();
    }
}