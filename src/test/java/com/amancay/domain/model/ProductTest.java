package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProductTest {

    private static final UUID V1 = UUID.randomUUID();
    private static final UUID V2 = UUID.randomUUID();

    @Test
    void replaceVariantsUpdatesKeepsOrderDropsMissingAndAppendsNew() {
        Product product = product();

        product.replaceVariants(List.of(
                new Product.VariantChange(null, new BigDecimal("30"), 3),
                new Product.VariantChange(V2, new BigDecimal("25"), 5)));

        assertThat(product.getVariants()).extracting(ProductVariant::getId).containsExactly(V2, null);
        assertThat(product.getVariants().getFirst().getPrice()).isEqualByComparingTo("25");
    }

    @Test
    void replaceVariantsWithNullRemovesThemAll() {
        Product product = product();

        product.replaceVariants(null);

        assertThat(product.getVariants()).isEmpty();
    }

    @Test
    void replaceVariantsRejectsAForeignVariant() {
        Product product = product();

        assertThatThrownBy(() -> product.replaceVariants(List.of(
                new Product.VariantChange(UUID.randomUUID(), BigDecimal.ONE, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaceImagesFollowsTheSameRules() {
        UUID image = UUID.randomUUID();
        Product product = new Product(UUID.randomUUID(), "Carpa", "carpa", null, null, true, null, null, List.of(),
                List.of(new ProductImage(image, "a.png")), Set.of(), null);

        product.replaceImages(List.of(new Product.ImageChange(image, "b.png"), new Product.ImageChange(null, "c.png")));

        assertThat(product.getImages()).extracting(ProductImage::getImageUrl).containsExactly("b.png", "c.png");
    }

    @Test
    void slugIsDerivedFromTheName() {
        assertThat(Slug.from("  Carpa Iglú 4 personas! ")).isEqualTo("carpa-iglu-4-personas");
        assertThat(Slug.from("¡¡!!")).hasSize(36);
    }

    private Product product() {
        return new Product(UUID.randomUUID(), "Carpa", "carpa", null, null, true, null, null, List.of(
                new ProductVariant(V1, new BigDecimal("10"), 1),
                new ProductVariant(V2, new BigDecimal("20"), 2)), List.of(), Set.of(), null);
    }
}
