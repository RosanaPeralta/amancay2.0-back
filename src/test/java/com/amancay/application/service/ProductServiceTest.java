package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.CreateProductCommand;
import com.amancay.application.port.in.UpdateProductCommand;
import com.amancay.application.service.fake.Admins;
import com.amancay.application.service.fake.InMemoryDiscountRepository;
import com.amancay.application.service.fake.InMemoryProductRepository;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;
import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.model.ProductVariant;

class ProductServiceTest {

    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InMemoryDiscountRepository discounts = new InMemoryDiscountRepository();
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(products, discounts, new DiscountService(discounts, products, Admins.guard()),
                Admins.guard());
    }

    @Test
    void createsProductWithVariantsAndGeneratesSlugFromName() {
        Product result = productService.create(ADMIN_ID, create("Coffee",
                List.of(new Product.NewVariant(new BigDecimal("10.50"), 4))));

        assertThat(result.getName()).isEqualTo("Coffee");
        assertThat(result.getSlug()).isEqualTo("coffee");
        assertThat(result.getVariants()).singleElement().satisfies(variant -> {
            assertThat(variant.getId()).isNotNull();
            assertThat(variant.getPrice()).isEqualByComparingTo("10.50");
        });
    }

    @Test
    void slugIgnoresAccentsAndSymbols() {
        assertThat(productService.create(ADMIN_ID, create("Café ¡Orgánico!", null)).getSlug()).isEqualTo("cafe-organico");
    }

    @Test
    void appendsSuffixWhenGeneratedSlugAlreadyExists() {
        productService.create(ADMIN_ID, create("Coffee", null));

        assertThat(productService.create(ADMIN_ID, create("Coffee", null)).getSlug()).isEqualTo("coffee-2");
        assertThat(productService.create(ADMIN_ID, create("Coffee", null)).getSlug()).isEqualTo("coffee-3");
    }

    @Test
    void updateKeepsItsOwnSlugWithoutASuffix() {
        Product coffee = productService.create(ADMIN_ID, create("Coffee", null));

        Product result = productService.update(ADMIN_ID, coffee.getId(), update("Coffee", List.of()));

        assertThat(result.getSlug()).isEqualTo("coffee");
    }

    @Test
    void updateSyncsVariantsById() {
        Product coffee = productService.create(ADMIN_ID, create("Coffee", List.of(
                new Product.NewVariant(new BigDecimal("10"), 1), new Product.NewVariant(new BigDecimal("20"), 2))));
        ProductVariant kept = coffee.getVariants().getFirst();

        Product result = productService.update(ADMIN_ID, coffee.getId(), update("Coffee", List.of(
                new Product.VariantChange(kept.getId(), new BigDecimal("15"), 9),
                new Product.VariantChange(null, new BigDecimal("30"), 3))));

        assertThat(result.getVariants()).extracting(variant -> variant.getPrice().intValue()).containsExactly(15, 30);
        assertThat(result.getVariants().getFirst().getId()).isEqualTo(kept.getId());
    }

    @Test
    void updateRejectsAVariantOfAnotherProduct() {
        Product coffee = productService.create(ADMIN_ID, create("Coffee", null));

        assertThatThrownBy(() -> productService.update(ADMIN_ID, coffee.getId(), update("Coffee", List.of(
                new Product.VariantChange(UUID.randomUUID(), BigDecimal.ONE, 1)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Variant does not belong to product");
    }

    @Test
    void returnsProductById() {
        Product coffee = productService.create(ADMIN_ID, create("Coffee", null));

        assertThat(productService.getById(coffee.getId()).getSlug()).isEqualTo("coffee");
    }

    @Test
    void unknownProductIsNotFound() {
        assertThatThrownBy(() -> productService.getById(UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> productService.delete(ADMIN_ID, UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listsProductsWithFiltersAndSort() {
        UUID category = UUID.randomUUID();
        products.add("Coffee", "coffee", true, Set.of(category));
        products.add("Cocoa", "cocoa", true, Set.of(category));
        products.add("Coffee beans", "coffee-beans", false, Set.of(category));
        products.add("Tea", "tea", true, Set.of());

        PageResult<ProductSummary> result = productService.list(new ProductFilter(" co ", category, true),
                ProductSort.NAME_DESC, new PageQuery(0, 12));

        assertThat(result.content()).extracting(ProductSummary::name).containsExactly("Coffee", "Cocoa");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void deletesAProduct() {
        Product coffee = productService.create(ADMIN_ID, create("Coffee", null));

        productService.delete(ADMIN_ID, coffee.getId());

        assertThat(products.existsById(coffee.getId())).isFalse();
    }

    private CreateProductCommand create(String name, List<Product.NewVariant> variants) {
        return new CreateProductCommand(name, "Short", "Description", true, variants, null, null, null);
    }

    private UpdateProductCommand update(String name, List<Product.VariantChange> variants) {
        return new UpdateProductCommand(name, null, null, true, variants, null, null, null);
    }
}
