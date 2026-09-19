package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amancay.dto.DiscountRequest;
import com.amancay.entity.Discount;
import com.amancay.entity.Product;
import com.amancay.repository.DiscountRepository;
import com.amancay.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private ProductRepository productRepository;

    private DiscountService discountService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        discountService = new DiscountService(discountRepository);
        productService = new ProductService(productRepository, discountRepository);
    }

    @Test
    void createsValidDiscount() {
        DiscountRequest request = new DiscountRequest(new BigDecimal("15.00"), "Descuento de temporada");
        when(discountRepository.save(any(Discount.class))).thenAnswer(invocation -> {
            Discount discount = invocation.getArgument(0);
            discount.setId(1L);
            return discount;
        });

        Discount result = discountService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPercentage()).isEqualByComparingTo("15.00");
        assertThat(result.getDescription()).isEqualTo("Descuento de temporada");
    }

    @Test
    void rejectsInvalidPercentages() {
        assertThatThrownBy(() -> discountService.create(new DiscountRequest(new BigDecimal("-1"), "Bad")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> discountService.create(new DiscountRequest(new BigDecimal("101"), "Bad")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> discountService.create(new DiscountRequest(null, "Bad")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesDiscount() {
        Discount discount = new Discount();
        discount.setId(2L);
        discount.setPercentage(new BigDecimal("10"));
        discount.setDescription("Viejo");

        when(discountRepository.findById(2L)).thenReturn(Optional.of(discount));
        when(discountRepository.save(any(Discount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = discountService.update(2L, new DiscountRequest(new BigDecimal("20"), "Nuevo"));

        assertThat(result.getPercentage()).isEqualByComparingTo("20");
        assertThat(result.getDescription()).isEqualTo("Nuevo");
    }

    @Test
    void assignsDiscountToProduct() {
        UUID productId = UUID.randomUUID();
        UUID discountId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setName("Laptop");
        product.setSlug("laptop");

        Discount discount = new Discount();
        discount.setId(3L);
        discount.setPercentage(new BigDecimal("15"));
        discount.setDescription("Flash sale");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(discountRepository.findById(3L)).thenReturn(Optional.of(discount));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.assignDiscount(productId, 3L);

        assertThat(result.getDiscount()).isNotNull();
        assertThat(result.getDiscount().getDescription()).isEqualTo("Flash sale");
    }

    @Test
    void removesDiscountFromProduct() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setName("Mouse");
        product.setSlug("mouse");

        Discount discount = new Discount();
        discount.setId(4L);
        discount.setDescription("Black Friday");
        discount.setPercentage(new BigDecimal("10"));
        product.setDiscount(discount);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.removeDiscount(productId);

        assertThat(result.getDiscount()).isNull();
    }

    @Test
    void calculatesFinalPriceWithDiscount() {
        Discount discount = new Discount();
        discount.setPercentage(new BigDecimal("15"));

        BigDecimal finalPrice = discountService.applyDiscount(BigDecimal.valueOf(100), discount);

        assertThat(finalPrice).isEqualByComparingTo("85.00");
    }
}
