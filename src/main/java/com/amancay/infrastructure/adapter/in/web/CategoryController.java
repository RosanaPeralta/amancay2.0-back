package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.CreateCategoryUseCase;
import com.amancay.application.port.in.DeleteCategoryUseCase;
import com.amancay.application.port.in.ListCategoriesQuery;
import com.amancay.application.port.in.UpdateCategoryUseCase;
import com.amancay.infrastructure.adapter.in.web.dto.CategoryResponse;
import com.amancay.infrastructure.adapter.in.web.dto.CreateCategoryRequest;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateCategoryRequest;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
@Validated
public class CategoryController {

    private final ListCategoriesQuery listCategoriesQuery;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryController(ListCategoriesQuery listCategoriesQuery, CreateCategoryUseCase createCategoryUseCase,
            UpdateCategoryUseCase updateCategoryUseCase, DeleteCategoryUseCase deleteCategoryUseCase) {
        this.listCategoriesQuery = listCategoriesQuery;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(listCategoriesQuery.listAll().stream().map(CategoryResponse::from).toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = CategoryResponse.from(createCategoryUseCase.create(loggedUser.id(), request.name()));
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(CategoryResponse.from(updateCategoryUseCase.rename(loggedUser.id(), id, request.name())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@AuthenticationPrincipal LoggedUser loggedUser, @PathVariable UUID id) {
        deleteCategoryUseCase.delete(loggedUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
