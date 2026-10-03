package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.service.fake.Admins;
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
        discountService = new DiscountService(discounts, products, Admins.guard());
        productService = new ProductService(products, discounts, discountService, Admins.guard());
    }

    @Test
    void createsValidDiscount() {
        Discount result = discountService.create(ADMIN_ID, new BigDecimal("15.00"), "Descuento de temporada");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPercentage()).isEqualByComparingTo("15.00");
        assertThat(result.getDescription()).isEqualTo("Descuento de temporada");
    }

    @Test
    void rejectsInvalidPercentages() {
        assertThatThrownBy(() -> discountService.create(ADMIN_ID, new BigDecimal("-1"), "Bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> discountService.create(ADMIN_ID, new BigDecimal("101"), "Bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> discountService.create(ADMIN_ID, null, "Bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(discounts.findAll()).isEmpty();
    }

    @Test
    void updatesDiscount() {
        Discount existing = discountService.create(ADMIN_ID, new BigDecimal("10"), "Viejo");

        Discount result = discountService.update(ADMIN_ID, existing.getId(), new BigDecimal("20"), "Nuevo");

        assertThat(result.getPercentage()).isEqualByComparingTo("20");
        assertThat(result.getDescription()).isEqualTo("Nuevo");
    }

    @Test
    void updatingAnUnknownDiscountIsNotFound() {
        assertThatThrownBy(() -> discountService.update(ADMIN_ID, 99L, new BigDecimal("20"), "Nuevo"))
                .isInstanceOf(DiscountNotFoundException.class);
    }

    @Test
    void searchWithBlankDescriptionListsAll() {
        discountService.create(ADMIN_ID, new BigDecimal("10"), "A");
        discountService.create(ADMIN_ID, new BigDecimal("20"), "B");

        assertThat(discountService.searchByDescription(" ")).hasSize(2);
        assertThat(discountService.searchByDescription("B")).extracting(Discount::getDescription).containsExactly("B");
    }

    @Test
    void cannotDeleteADiscountThatIsAssignedToAProduct() {
        Discount discount = discountService.create(ADMIN_ID, new BigDecimal("15"), "Flash sale");
        Product product = products.add("Laptop", "laptop", true, Set.of());
        productService.assignDiscount(ADMIN_ID, product.getId(), discount.getId());

        assertThatThrownBy(() -> discountService.delete(ADMIN_ID, discount.getId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(discounts.findById(discount.getId())).isPresent();
    }

    @Test
    void deletesAnUnusedDiscount() {
        Discount discount = discountService.create(ADMIN_ID, new BigDecimal("15"), "Flash sale");

        discountService.delete(ADMIN_ID, discount.getId());

        assertThat(discounts.findById(discount.getId())).isEmpty();
    }

    @Test
    void assignsDiscountToProduct() {
        Product product = products.add("Laptop", "laptop", true, Set.of());
        Discount discount = discountService.create(ADMIN_ID, new BigDecimal("15"), "Flash sale");

        Product result = productService.assignDiscount(ADMIN_ID, product.getId(), discount.getId());

        assertThat(result.getDiscount()).isNotNull();
        assertThat(result.getDiscount().getDescription()).isEqualTo("Flash sale");
    }

    @Test
    void assigningAnUnknownDiscountIsNotFound() {
        Product product = products.add("Laptop", "laptop", true, Set.of());

        assertThatThrownBy(() -> productService.assignDiscount(ADMIN_ID, product.getId(), 99L))
                .isInstanceOf(DiscountNotFoundException.class);
    }

    @Test
    void removesDiscountFromProduct() {
        Product product = products.add("Mouse", "mouse", true, Set.of());
        Discount discount = discountService.create(ADMIN_ID, new BigDecimal("10"), "Black Friday");
        productService.assignDiscount(ADMIN_ID, product.getId(), discount.getId());

        Product result = productService.removeDiscount(ADMIN_ID, product.getId());

        assertThat(result.getDiscount()).isNull();
    }

    @Test
    void createAndAssignCreatesTheDiscountAndLinksIt() {
        Product product = products.add("Mouse", "mouse", true, Set.of());

        Product result = productService.createAndAssignDiscount(ADMIN_ID, product.getId(), new BigDecimal("25"), "Hot Sale");

        assertThat(result.getDiscount().getDescription()).isEqualTo("Hot Sale");
        assertThat(discounts.findAll()).hasSize(1);
    }

    @Test
    void createAndAssignOnAnUnknownProductIsNotFound() {
        // En produccion la transaccion revierte el descuento recien creado.
        assertThatThrownBy(() -> productService.createAndAssignDiscount(ADMIN_ID, UUID.randomUUID(), new BigDecimal("25"),
                "Hot Sale"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void calculatesFinalPriceWithDiscount() {
        Discount discount = Discount.create(new BigDecimal("15"), "Promo");

        assertThat(discount.applyTo(BigDecimal.valueOf(100))).isEqualByComparingTo("85.00");
    }
}
