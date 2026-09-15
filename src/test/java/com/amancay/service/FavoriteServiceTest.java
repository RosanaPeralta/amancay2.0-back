package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.amancay.dto.FavoriteDto;
import com.amancay.dto.PageResponse;
import com.amancay.entity.Favorite;
import com.amancay.entity.Product;
import com.amancay.exceptions.DuplicateFavoriteException;
import com.amancay.exceptions.FavoriteNotFoundException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.repository.FavoriteRepository;
import com.amancay.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductRepository productRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, productRepository);
    }

    @Test
    void addsAFavoriteWithProductData() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(favoriteRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.saveAndFlush(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FavoriteDto result = favoriteService.add(USER_ID, PRODUCT_ID);

        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.name()).isEqualTo("Carpa");
        assertThat(result.slug()).isEqualTo("carpa");
        assertThat(result.active()).isTrue();
    }

    @Test
    void rejectsUnknownProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.add(USER_ID, PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);
        verify(favoriteRepository, never()).saveAndFlush(any(Favorite.class));
    }

    @Test
    void rejectsDuplicateFavorite() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(favoriteRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.add(USER_ID, PRODUCT_ID))
                .isInstanceOf(DuplicateFavoriteException.class);
        verify(favoriteRepository, never()).saveAndFlush(any(Favorite.class));
    }

    @Test
    void removesAnExistingFavorite() {
        Favorite favorite = Favorite.of(USER_ID, PRODUCT_ID);
        when(favoriteRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(favorite));

        favoriteService.remove(USER_ID, PRODUCT_ID);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void removingAMissingFavoriteIsNotFound() {
        when(favoriteRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.remove(USER_ID, PRODUCT_ID))
                .isInstanceOf(FavoriteNotFoundException.class);
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    void listsFavoritesResolvingProductsInOneQuery() {
        when(favoriteRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Favorite.of(USER_ID, PRODUCT_ID)), PageRequest.of(0, 20), 1));
        when(productRepository.findAllById(any())).thenReturn(List.of(product()));

        PageResponse<FavoriteDto> result = favoriteService.list(USER_ID, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().name()).isEqualTo("Carpa");
        assertThat(result.totalElements()).isEqualTo(1L);
        verify(productRepository, never()).findById(any());
    }

    @Test
    void listsOnlyProductIds() {
        when(favoriteRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of(PRODUCT_ID));

        assertThat(favoriteService.listProductIds(USER_ID)).containsExactly(PRODUCT_ID);
    }

    private Product product() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Carpa");
        product.setSlug("carpa");
        product.setActive(true);
        return product;
    }
}
