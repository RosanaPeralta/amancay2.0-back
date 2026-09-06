package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amancay.dto.CreateProductRequest;
import com.amancay.dto.ProductDto;
import com.amancay.entity.Product;
import com.amancay.exceptions.DuplicateSlugException;
import com.amancay.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createsProductWithVariants() {
        CreateProductRequest request = new CreateProductRequest(
                "Coffee", "coffee", "Short", "Description", true,
                java.util.List.of(new CreateProductRequest.VariantRequest(new BigDecimal("10.50"), 4)));
        when(productRepository.existsBySlug("coffee")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto result = productService.createProduct(request);

        assertThat(result.name()).isEqualTo("Coffee");
        assertThat(result.variants()).hasSize(1);
        assertThat(result.variants().getFirst().price()).isEqualByComparingTo("10.50");
    }

    @Test
    void rejectsDuplicateSlug() {
        when(productRepository.existsBySlug("coffee")).thenReturn(true);
        CreateProductRequest request = new CreateProductRequest("Coffee", "coffee", null, null, true, null);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSlugException.class);
    }

    @Test
    void returnsProductById() {
        Product product = new Product();
        product.setName("Coffee");
        product.setSlug("coffee");
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        ProductDto result = productService.getProductById(java.util.UUID.randomUUID());

        assertThat(result.slug()).isEqualTo("coffee");
    }
}
