package com.amancay.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Protege las invariantes de la factory estatica {@link Review#publish}.
 *
 * <p>Que cubre: que una resenia recien creada nazca completa y valida. La factory es el unico
 * camino para construir una resenia nueva, asi que estas pruebas son las que garantizan que no
 * exista un {@code Review} a medio armar en ningun punto del programa.
 *
 * <p>Las validaciones de rango tambien viven en las anotaciones de Bean Validation del DTO, y ahi
 * devuelven 400 antes de llegar al dominio. La duplicacion es deliberada: el DTO protege el borde
 * HTTP, la factory protege el dominio de cualquier otro invocador (un job, una migracion, un
 * modulo futuro) que no pase por un controller.
 *
 * <p>Cuando se puede eliminar: solo si desaparece la factory. Mientras la construccion de resenias
 * tenga reglas, este es el lugar donde se verifican.
 */
class ReviewTest {

    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void publishCreatesACompleteReview() {
        Review review = Review.publish(PRODUCT_ID, USER_ID, 5, "Excelente", "La recompro");

        assertThat(review.getId()).isNotNull();
        assertThat(review.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(review.getUserId()).isEqualTo(USER_ID);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getTitle()).isEqualTo("Excelente");
        assertThat(review.getComment()).isEqualTo("La recompro");
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void publishAssignsADifferentIdToEachReview() {
        Review first = Review.publish(PRODUCT_ID, USER_ID, 4, null, null);
        Review second = Review.publish(PRODUCT_ID, USER_ID, 4, null, null);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void publishAcceptsAReviewWithoutTitleOrComment() {
        Review review = Review.publish(PRODUCT_ID, USER_ID, 3, null, null);

        assertThat(review.getTitle()).isNull();
        assertThat(review.getComment()).isNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void publishRejectsARatingBelowTheAllowedRange() {
        assertThatThrownBy(() -> Review.publish(PRODUCT_ID, USER_ID, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rating");
    }

    @Test
    void publishRejectsARatingAboveTheAllowedRange() {
        assertThatThrownBy(() -> Review.publish(PRODUCT_ID, USER_ID, 6, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rating");
    }

    @Test
    void publishRejectsATitleLongerThanTheColumn() {
        String tooLong = "x".repeat(151);

        assertThatThrownBy(() -> Review.publish(PRODUCT_ID, USER_ID, 4, tooLong, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void publishRejectsAMissingProduct() {
        assertThatThrownBy(() -> Review.publish(null, USER_ID, 4, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void publishRejectsAMissingUser() {
        assertThatThrownBy(() -> Review.publish(PRODUCT_ID, null, 4, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
