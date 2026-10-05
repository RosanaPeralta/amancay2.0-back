package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.CreateCategoryUseCase;
import com.amancay.application.port.in.DeleteCategoryUseCase;
import com.amancay.application.port.in.ListCategoriesQuery;
import com.amancay.application.port.in.UpdateCategoryUseCase;
import com.amancay.application.port.out.CategoryRepositoryPort;
import com.amancay.domain.exception.CategoryNotFoundException;
import com.amancay.domain.exception.DuplicateCategoryNameException;
import com.amancay.domain.model.Category;

@Service
public class CategoryService
        implements ListCategoriesQuery, CreateCategoryUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase {

    private final CategoryRepositoryPort categoryRepository;
    private final AdminGuard adminGuard;

    public CategoryService(CategoryRepositoryPort categoryRepository, AdminGuard adminGuard) {
        this.categoryRepository = categoryRepository;
        this.adminGuard = adminGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public Category create(UUID requesterId, String name) {
        adminGuard.requireAdmin(requesterId);
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateCategoryNameException(name);
        }
        return categoryRepository.save(Category.create(name));
    }

    @Override
    @Transactional
    public Category rename(UUID requesterId, UUID id, String name) {
        adminGuard.requireAdmin(requesterId);
        Category category = findCategory(id);
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new DuplicateCategoryNameException(name);
        }
        category.rename(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void delete(UUID requesterId, UUID id) {
        adminGuard.requireAdmin(requesterId);
        findCategory(id);
        categoryRepository.deleteById(id);
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }
}
