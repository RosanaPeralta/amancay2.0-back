package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.service.fake.InMemoryCategoryRepository;
import com.amancay.domain.exception.CategoryNotFoundException;
import com.amancay.domain.exception.DuplicateCategoryNameException;
import com.amancay.domain.model.Category;

class CategoryServiceTest {

    private final InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categories);
    }

    @Test
    void createsAndListsCategories() {
        Category created = categoryService.create("Camping");

        assertThat(created.getId()).isNotNull();
        assertThat(categoryService.listAll()).extracting(Category::getName).containsExactly("Camping");
    }

    @Test
    void rejectsDuplicateNames() {
        categoryService.create("Camping");

        assertThatThrownBy(() -> categoryService.create("Camping")).isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void renamesUnlessAnotherCategoryHasTheName() {
        Category camping = categoryService.create("Camping");
        categoryService.create("Trekking");

        assertThat(categoryService.rename(camping.getId(), "Camping").getName()).isEqualTo("Camping");
        assertThatThrownBy(() -> categoryService.rename(camping.getId(), "Trekking"))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void deletesAndReportsUnknownCategories() {
        Category camping = categoryService.create("Camping");

        categoryService.delete(camping.getId());

        assertThat(categoryService.listAll()).isEmpty();
        assertThatThrownBy(() -> categoryService.delete(UUID.randomUUID()))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
