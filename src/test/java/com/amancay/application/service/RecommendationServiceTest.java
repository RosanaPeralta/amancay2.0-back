package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.ProductRecommendations;
import com.amancay.application.port.out.ProductRecommenderPort;
import com.amancay.application.service.fake.InMemoryFavoriteRepository;
import com.amancay.application.service.fake.InMemoryProductRepository;
import com.amancay.domain.exception.RecommenderUnavailableException;
import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.RecommendationCandidate;
import com.amancay.domain.model.RecommendationSource;

class RecommendationServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CAMPING = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TREKKING = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    private final InMemoryFavoriteRepository favorites = new InMemoryFavoriteRepository();
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final List<Product> catalog = new ArrayList<>();
    private final FakeRecommender recommender = new FakeRecommender();
    private RecommendationService service;

    private Product carpa;
    private Product bolsaDeDormir;
    private Product bastones;
    private Product linterna;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(favorites, this::candidates, recommender, products,
                Duration.ofMinutes(30));
        carpa = add("Carpa", Set.of(CAMPING));
        bolsaDeDormir = add("Bolsa de dormir", Set.of(CAMPING));
        bastones = add("Bastones", Set.of(TREKKING));
        linterna = add("Linterna", Set.of(CAMPING, TREKKING));
    }

    @Test
    void withoutFavoritesThereIsNothingToRecommend() {
        ProductRecommendations result = service.recommend(USER_ID, 8);

        assertThat(result.source()).isEqualTo(RecommendationSource.NONE);
        assertThat(result.products()).isEmpty();
        assertThat(recommender.calls).isZero();
    }

    @Test
    void usesTheAiRankingAndDropsFavoritesUnknownIdsAndDuplicates() {
        favorite(carpa);
        recommender.answer = List.of(bastones.getId(), UUID.randomUUID(), carpa.getId(), bastones.getId(),
                linterna.getId());

        ProductRecommendations result = service.recommend(USER_ID, 8);

        assertThat(result.source()).isEqualTo(RecommendationSource.AI);
        assertThat(result.favoritesCount()).isEqualTo(1);
        assertThat(result.products()).extracting(Product::getId).containsExactly(bastones.getId(), linterna.getId());
    }

    @Test
    void fallsBackToSharedCategoriesWhenTheAiIsUnavailable() {
        favorite(carpa);
        recommender.unavailable = true;

        ProductRecommendations result = service.recommend(USER_ID, 8);

        assertThat(result.source()).isEqualTo(RecommendationSource.CATEGORIES);
        // Bastones no comparte categoria con la carpa: no se sugiere.
        assertThat(result.products()).extracting(Product::getName).containsExactly("Bolsa de dormir", "Linterna");
    }

    @Test
    void reusesTheAiRankingWhileFavoritesDoNotChange() {
        favorite(carpa);
        recommender.answer = List.of(linterna.getId(), bastones.getId());

        service.recommend(USER_ID, 8);
        ProductRecommendations second = service.recommend(USER_ID, 1);

        assertThat(recommender.calls).isEqualTo(1);
        assertThat(second.products()).extracting(Product::getId).containsExactly(linterna.getId());

        favorite(bastones);
        service.recommend(USER_ID, 8);

        assertThat(recommender.calls).isEqualTo(2);
    }

    @Test
    void sendsAtMostFiveRandomFavoritesButNeverSuggestsAnyFavorite() {
        List<Product> many = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Product product = add("Extra " + i, Set.of(CAMPING));
            favorite(product);
            many.add(product);
        }
        favorite(carpa);
        Set<Set<UUID>> samples = new HashSet<>();

        for (int i = 0; i < 30; i++) {
            ProductRecommendations result = service.recommend(USER_ID, 8);

            assertThat(recommender.lastFavorites).hasSize(RecommendationService.MAX_FAVORITES_SAMPLE);
            assertThat(recommender.lastCandidates).extracting(RecommendationCandidate::id)
                    .containsExactlyInAnyOrder(bolsaDeDormir.getId(), bastones.getId(), linterna.getId());
            assertThat(result.favoritesCount()).isEqualTo(8);
            assertThat(result.basedOnCount()).isEqualTo(RecommendationService.MAX_FAVORITES_SAMPLE);
            samples.add(recommender.lastFavorites.stream().map(RecommendationCandidate::id).collect(Collectors.toSet()));
        }

        // 8 favoritos tomados de a 5: en 30 pedidos tiene que haber salido mas de una combinacion.
        assertThat(samples).hasSizeGreaterThan(1);
    }

    private Product add(String name, Set<UUID> categoryIds) {
        Product product = products.create(name, name.toLowerCase().replace(' ', '-'), true, categoryIds);
        catalog.add(product);
        return product;
    }

    private void favorite(Product product) {
        favorites.save(Favorite.of(USER_ID, product.getId()));
    }

    private List<RecommendationCandidate> candidates() {
        return catalog.stream().map(p -> new RecommendationCandidate(p.getId(), p.getName(), null,
                p.getCategoryIds(), List.of())).toList();
    }

    private static class FakeRecommender implements ProductRecommenderPort {
        List<UUID> answer = List.of();
        boolean unavailable;
        int calls;
        List<RecommendationCandidate> lastFavorites = List.of();
        List<RecommendationCandidate> lastCandidates = List.of();

        @Override
        public List<UUID> rank(List<RecommendationCandidate> favorites, List<RecommendationCandidate> candidates,
                int limit) {
            calls++;
            lastFavorites = favorites;
            lastCandidates = candidates;
            if (unavailable) {
                throw new RecommenderUnavailableException("not configured");
            }
            return answer;
        }
    }
}
