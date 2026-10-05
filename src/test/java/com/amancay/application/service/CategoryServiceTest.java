package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.service.fake.Admins;
import com.amancay.application.service.fake.InMemoryCategoryRepository;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.CategoryNotFoundException;
import com.amancay.domain.exception.DuplicateCategoryNameException;
import com.amancay.domain.model.Category;

class CategoryServiceTest {

    private final InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categories, Admins.guard());
    }

    @Test
    void buyerCannotCreateRenameOrDeleteCategories() {
        Category camping = categoryService.create(ADMIN_ID, "Camping");
        UUID buyerId = UUID.randomUUID();

        assertThatThrownBy(() -> categoryService.create(buyerId, "Pesca")).isInstanceOf(AdminRequiredException.class);
        assertThatThrownBy(() -> categoryService.rename(buyerId, camping.getId(), "Pesca"))
                .isInstanceOf(AdminRequiredException.class);
        assertThatThrownBy(() -> categoryService.delete(buyerId, camping.getId()))
                .isInstanceOf(AdminRequiredException.class);
        assertThat(categoryService.listAll()).extracting(Category::getName).containsExactly("Camping");
    }

    @Test
    void createsAndListsCategories() {
        Category created = categoryService.create(ADMIN_ID, "Camping");

        assertThat(created.getId()).isNotNull();
        assertThat(categoryService.listAll()).extracting(Category::getName).containsExactly("Camping");
    }

    @Test
    void rejectsDuplicateNames() {
        categoryService.create(ADMIN_ID, "Camping");

        assertThatThrownBy(() -> categoryService.create(ADMIN_ID, "Camping")).isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void renamesUnlessAnotherCategoryHasTheName() {
        Category camping = categoryService.create(ADMIN_ID, "Camping");
        categoryService.create(ADMIN_ID, "Trekking");

        assertThat(categoryService.rename(ADMIN_ID, camping.getId(), "Camping").getName()).isEqualTo("Camping");
        assertThatThrownBy(() -> categoryService.rename(ADMIN_ID, camping.getId(), "Trekking"))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void deletesAndReportsUnknownCategories() {
        Category camping = categoryService.create(ADMIN_ID, "Camping");

        categoryService.delete(ADMIN_ID, camping.getId());

        assertThat(categoryService.listAll()).isEmpty();
        assertThatThrownBy(() -> categoryService.delete(ADMIN_ID, UUID.randomUUID()))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
