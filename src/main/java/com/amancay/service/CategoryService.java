package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.amancay.dto.CategoryDto;
import com.amancay.dto.CreateCategoryRequest;
import com.amancay.dto.UpdateCategoryRequest;
import com.amancay.entity.Category;
import com.amancay.exceptions.CategoryNotFoundException;
import com.amancay.exceptions.DuplicateCategoryNameException;
import com.amancay.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryDto createCategory(CreateCategoryRequest request) {
        validateName(request.name());

        Category category = new Category();
        category.setName(request.name());

        Category saved = categoryRepository.save(category);

        return toDto(saved);
    }

    public CategoryDto updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = findCategory(id);

        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateCategoryNameException(request.name());
        }

        category.setName(request.name());

        Category updated = categoryRepository.save(category);

        return toDto(updated);
    }

    public void deleteCategory(UUID id) {
        Category category = findCategory(id);

        categoryRepository.delete(category);
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private void validateName(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateCategoryNameException(name);
        }
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName());
    }
}