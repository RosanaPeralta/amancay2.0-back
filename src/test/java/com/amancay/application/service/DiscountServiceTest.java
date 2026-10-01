package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.service.fake.InMemoryDiscountRepository;
import com.amancay.application.service.fake.InMemoryProductRepository;
import com.amancay.domain.exception.DiscountNotFoundException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.model.Discount;
import com.amancay.domain.model.Product;

class DiscountServiceTest {

    private final InMemoryDiscountRepository discounts = new InMemoryDiscountRepository();
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private DiscountService discountService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        discountService = new DiscountService(discounts, products);
        productService = new ProductService(products, discounts, discountService);
    }

    @Test
    void createsValidDiscount() {
        Discount result = discountService.create(new BigDecimal("15.00"), "Descuento de temporada");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPercentage()).isEqualByComparingTo("15.00");
        assertThat(result.getDescription()).isEqualTo("Descuento de temporada");
    }

    @Test
    void rejectsInvalidPercentages() {
        assertThatThrownBy(() -> discountService.create(new BigDecimal("-1"), "Bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> discountService.create(new BigDecimal("101"), "Bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> discountService.create(null, "Bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(discounts.findAll()).isEmpty();
    }

    @Test
    void updatesDiscount() {
        Discount existing = discountService.create(new BigDecimal("10"), "Viejo");

        Discount result = discountService.update(existing.getId(), new BigDecimal("20"), "Nuevo");

        assertThat(result.getPercentage()).isEqualByComparingTo("20");
        assertThat(result.getDescription()).isEqualTo("Nuevo");
    }

    @Test
    void updatingAnUnknownDiscountIsNotFound() {
        assertThatThrownBy(() -> discountService.update(99L, new BigDecimal("20"), "Nuevo"))
                .isInstanceOf(DiscountNotFoundException.class);
    }

    @Test
    void searchWithBlankDescriptionListsAll() {
        discountService.create(new BigDecimal("10"), "A");
        discountService.create(new BigDecimal("20"), "B");

        assertThat(discountService.searchByDescription(" ")).hasSize(2);
        assertThat(discountService.searchByDescription("B")).extracting(Discount::getDescription).containsExactly("B");
    }

    @Test
    void cannotDeleteADiscountThatIsAssignedToAProduct() {
        Discount discount = discountService.create(new BigDecimal("15"), "Flash sale");
        Product product = products.add("Laptop", "laptop", true, Set.of());
        productService.assignDiscount(product.getId(), discount.getId());

        assertThatThrownBy(() -> discountService.delete(discount.getId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(discounts.findById(discount.getId())).isPresent();
    }

    @Test
    void deletesAnUnusedDiscount() {
        Discount discount = discountService.create(new BigDecimal("15"), "Flash sale");

        discountService.delete(discount.getId());

        assertThat(discounts.findById(discount.getId())).isEmpty();
    }

    @Test
    void assignsDiscountToProduct() {
        Product product = products.add("Laptop", "laptop", true, Set.of());
        Discount discount = discountService.create(new BigDecimal("15"), "Flash sale");

        Product result = productService.assignDiscount(product.getId(), discount.getId());

        assertThat(result.getDiscount()).isNotNull();
        assertThat(result.getDiscount().getDescription()).isEqualTo("Flash sale");
    }

    @Test
    void assigningAnUnknownDiscountIsNotFound() {
        Product product = products.add("Laptop", "laptop", true, Set.of());

        assertThatThrownBy(() -> productService.assignDiscount(product.getId(), 99L))
                .isInstanceOf(DiscountNotFoundException.class);
    }

    @Test
    void removesDiscountFromProduct() {
        Product product = products.add("Mouse", "mouse", true, Set.of());
        Discount discount = discountService.create(new BigDecimal("10"), "Black Friday");
        productService.assignDiscount(product.getId(), discount.getId());

        Product result = productService.removeDiscount(product.getId());

        assertThat(result.getDiscount()).isNull();
    }

    @Test
    void createAndAssignCreatesTheDiscountAndLinksIt() {
        Product product = products.add("Mouse", "mouse", true, Set.of());

        Product result = productService.createAndAssignDiscount(product.getId(), new BigDecimal("25"), "Hot Sale");

        assertThat(result.getDiscount().getDescription()).isEqualTo("Hot Sale");
        assertThat(discounts.findAll()).hasSize(1);
    }

    @Test
    void createAndAssignOnAnUnknownProductIsNotFound() {
        // En produccion la transaccion revierte el descuento recien creado.
        assertThatThrownBy(() -> productService.createAndAssignDiscount(UUID.randomUUID(), new BigDecimal("25"),
                "Hot Sale"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void calculatesFinalPriceWithDiscount() {
        Discount discount = Discount.create(new BigDecimal("15"), "Promo");

        assertThat(discount.applyTo(BigDecimal.valueOf(100))).isEqualByComparingTo("85.00");
    }
}
