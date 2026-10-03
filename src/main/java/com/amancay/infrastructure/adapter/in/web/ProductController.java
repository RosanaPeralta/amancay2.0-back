package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.CreateProductUseCase;
import com.amancay.application.port.in.DeleteProductUseCase;
import com.amancay.application.port.in.GetProductQuery;
import com.amancay.application.port.in.ListProductsQuery;
import com.amancay.application.port.in.ManageProductDiscountUseCase;
import com.amancay.application.port.in.UpdateProductUseCase;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;
import com.amancay.infrastructure.adapter.in.web.dto.CreateProductRequest;
import com.amancay.infrastructure.adapter.in.web.dto.DiscountRequest;
import com.amancay.infrastructure.adapter.in.web.dto.PageResponse;
import com.amancay.infrastructure.adapter.in.web.dto.ProductResponse;
import com.amancay.infrastructure.adapter.in.web.dto.ProductSummaryResponse;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateProductRequest;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ListProductsQuery listProductsQuery;
    private final GetProductQuery getProductQuery;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ManageProductDiscountUseCase manageProductDiscountUseCase;

    public ProductController(ListProductsQuery listProductsQuery, GetProductQuery getProductQuery,
            CreateProductUseCase createProductUseCase, UpdateProductUseCase updateProductUseCase,
            DeleteProductUseCase deleteProductUseCase, ManageProductDiscountUseCase manageProductDiscountUseCase) {
        this.listProductsQuery = listProductsQuery;
        this.getProductQuery = getProductQuery;
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.manageProductDiscountUseCase = manageProductDiscountUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(name = "category_id", required = false) UUID categoryId,
            @RequestParam(name = "is_active", required = false) Boolean isActive,
            @RequestParam(defaultValue = "name") String sort) {
        return ResponseEntity.ok(PageResponse.from(listProductsQuery.list(new ProductFilter(name, categoryId, isActive),
                ProductSort.from(sort), new PageQuery(page, size)), ProductSummaryResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ProductResponse.from(getProductQuery.getById(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = ProductResponse.from(createProductUseCase.create(loggedUser.id(), request.toCommand()));
        return ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ProductResponse.from(updateProductUseCase.update(loggedUser.id(), id,
                request.toCommand())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoggedUser loggedUser, @PathVariable UUID id) {
        deleteProductUseCase.delete(loggedUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{productId}/discounts")
    public ResponseEntity<ProductResponse> createAndAssignDiscount(@AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId, @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(ProductResponse.from(manageProductDiscountUseCase.createAndAssignDiscount(
                loggedUser.id(), productId, request.percentage(), request.description())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{productId}/discounts/{discountId}")
    public ResponseEntity<ProductResponse> assignExistingDiscount(@AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId, @PathVariable Long discountId) {
        return ResponseEntity.ok(ProductResponse.from(manageProductDiscountUseCase.assignDiscount(loggedUser.id(),
                productId, discountId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{productId}/discounts")
    public ResponseEntity<Void> removeDiscount(@AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId) {
        manageProductDiscountUseCase.removeDiscount(loggedUser.id(), productId);
        return ResponseEntity.noContent().build();
    }
}
