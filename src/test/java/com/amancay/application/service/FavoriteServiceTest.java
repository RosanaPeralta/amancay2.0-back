package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.FavoriteProduct;
import com.amancay.application.service.fake.InMemoryFavoriteRepository;
import com.amancay.application.service.fake.InMemoryProductRepository;
import com.amancay.domain.exception.DuplicateFavoriteException;
import com.amancay.domain.exception.FavoriteNotFoundException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;

class FavoriteServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final InMemoryFavoriteRepository favorites = new InMemoryFavoriteRepository();
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private FavoriteService favoriteService;
    private Product carpa;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favorites, products);
        carpa = products.add("Carpa", "carpa", true, Set.of());
    }

    @Test
    void addsAFavoriteWithProductData() {
        FavoriteProduct result = favoriteService.add(USER_ID, carpa.getId());

        assertThat(result.favorite().getProductId()).isEqualTo(carpa.getId());
        assertThat(result.product().name()).isEqualTo("Carpa");
        assertThat(result.product().slug()).isEqualTo("carpa");
        assertThat(result.product().active()).isTrue();
        assertThat(result.favorite().getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsUnknownProduct() {
        assertThatThrownBy(() -> favoriteService.add(USER_ID, UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);
        assertThat(favorites.calls).doesNotContain("save");
    }

    @Test
    void rejectsDuplicateFavorite() {
        favoriteService.add(USER_ID, carpa.getId());
        favorites.calls.clear();

        assertThatThrownBy(() -> favoriteService.add(USER_ID, carpa.getId()))
                .isInstanceOf(DuplicateFavoriteException.class);
        assertThat(favorites.calls).doesNotContain("save");
    }

    @Test
    void removesAnExistingFavorite() {
        favoriteService.add(USER_ID, carpa.getId());

        favoriteService.remove(USER_ID, carpa.getId());

        assertThat(favorites.size()).isZero();
    }

    @Test
    void removingAMissingFavoriteIsNotFound() {
        assertThatThrownBy(() -> favoriteService.remove(USER_ID, carpa.getId()))
                .isInstanceOf(FavoriteNotFoundException.class);
        assertThat(favorites.calls).doesNotContain("delete");
    }

    @Test
    void listsFavoritesResolvingProductsInOneQuery() {
        Product mochila = products.add("Mochila", "mochila", true, Set.of());
        favoriteService.add(USER_ID, carpa.getId());
        favoriteService.add(USER_ID, mochila.getId());
        products.calls.clear();

        PageResult<FavoriteProduct> result = favoriteService.list(USER_ID, new PageQuery(0, 20));

        assertThat(result.content()).extracting(item -> item.product().name()).containsExactly("Mochila", "Carpa");
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(products.count("findSummariesById")).isEqualTo(1);
        assertThat(products.count("findSummaryById")).isZero();
    }

    @Test
    void listsOnlyProductIds() {
        favoriteService.add(USER_ID, carpa.getId());

        assertThat(favoriteService.listProductIds(USER_ID)).containsExactly(carpa.getId());
    }
}
