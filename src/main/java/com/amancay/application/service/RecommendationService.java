package com.amancay.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amancay.application.port.in.ProductRecommendations;
import com.amancay.application.port.in.RecommendProductsQuery;
import com.amancay.application.port.out.FavoriteRepositoryPort;
import com.amancay.application.port.out.LoadRecommendationCandidatesPort;
import com.amancay.application.port.out.ProductRepositoryPort;
import com.amancay.application.port.out.ProductRecommenderPort;
import com.amancay.domain.exception.RecommenderUnavailableException;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.RecommendationCandidate;
import com.amancay.domain.model.RecommendationSource;

// Sin @Transactional a proposito: no queremos una conexion tomada mientras esperamos a la IA.
@Service
public class RecommendationService implements RecommendProductsQuery {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    public static final int MAX_LIMIT = 24;
    // Favoritos que se le pasan a la IA por pedido. Se eligen al azar para que las sugerencias
    // vayan variando entre visitas y el prompt quede chico.
    public static final int MAX_FAVORITES_SAMPLE = 5;

    private final FavoriteRepositoryPort favoriteRepository;
    private final LoadRecommendationCandidatesPort candidatesPort;
    private final ProductRecommenderPort recommender;
    private final ProductRepositoryPort productRepository;
    private final Duration cacheTtl;
    // Por usuario: el ranking de la IA para la ultima muestra de favoritos, mientras no venza el TTL.
    // Con 5 favoritos o menos la muestra es siempre la misma y el cache se reusa.
    private final Map<UUID, CachedRanking> cache = new ConcurrentHashMap<>();

    public RecommendationService(FavoriteRepositoryPort favoriteRepository,
            LoadRecommendationCandidatesPort candidatesPort, ProductRecommenderPort recommender,
            ProductRepositoryPort productRepository,
            @Value("${amancay.ai.recommendations.cache-ttl:PT30M}") Duration cacheTtl) {
        this.favoriteRepository = favoriteRepository;
        this.candidatesPort = candidatesPort;
        this.recommender = recommender;
        this.productRepository = productRepository;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public ProductRecommendations recommend(UUID userId, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<UUID> favoriteIds = favoriteRepository.findProductIdsByUserId(userId);
        if (favoriteIds.isEmpty()) {
            return new ProductRecommendations(List.of(), RecommendationSource.NONE, 0, 0);
        }

        Set<UUID> favoriteSet = new HashSet<>(favoriteIds);
        List<RecommendationCandidate> active = candidatesPort.findActive();
        List<RecommendationCandidate> favorites = sample(
                active.stream().filter(c -> favoriteSet.contains(c.id())).toList());
        // Se excluyen todos los favoritos, no solo la muestra: nunca sugerimos algo que ya marco.
        List<RecommendationCandidate> candidates = active.stream().filter(c -> !favoriteSet.contains(c.id())).toList();
        if (favorites.isEmpty() || candidates.isEmpty()) {
            return new ProductRecommendations(List.of(), RecommendationSource.NONE, favoriteIds.size(), 0);
        }

        Set<UUID> candidateIds = candidates.stream().map(RecommendationCandidate::id).collect(Collectors.toSet());
        List<UUID> ranked = rankWithAi(userId, favorites, candidates, candidateIds, size);
        RecommendationSource source = RecommendationSource.AI;
        if (ranked.isEmpty()) {
            ranked = rankByCategories(favorites, candidates, size);
            source = RecommendationSource.CATEGORIES;
        }
        return new ProductRecommendations(loadProducts(ranked), source, favoriteIds.size(), favorites.size());
    }

    private static List<RecommendationCandidate> sample(List<RecommendationCandidate> favorites) {
        if (favorites.size() <= MAX_FAVORITES_SAMPLE) {
            return favorites;
        }
        List<RecommendationCandidate> shuffled = new ArrayList<>(favorites);
        Collections.shuffle(shuffled);
        return List.copyOf(shuffled.subList(0, MAX_FAVORITES_SAMPLE));
    }

    private List<UUID> rankWithAi(UUID userId, List<RecommendationCandidate> favorites,
            List<RecommendationCandidate> candidates, Set<UUID> candidateIds, int size) {
        String fingerprint = favorites.stream().map(f -> f.id().toString()).sorted().collect(Collectors.joining(","));
        CachedRanking cached = cache.get(userId);
        if (cached != null && cached.fingerprint().equals(fingerprint) && cached.expiresAt().isAfter(Instant.now())) {
            return onlyCandidates(cached.ids(), candidateIds, size);
        }

        try {
            // Pedimos el maximo para que un limit distinto reuse el mismo ranking cacheado.
            List<UUID> ids = onlyCandidates(recommender.rank(favorites, candidates, MAX_LIMIT), candidateIds,
                    MAX_LIMIT);
            if (!ids.isEmpty()) {
                cache.put(userId, new CachedRanking(fingerprint, ids, Instant.now().plus(cacheTtl)));
            }
            return ids.stream().limit(size).toList();
        } catch (RecommenderUnavailableException e) {
            log.warn("AI recommender unavailable, falling back to categories: {}", e.getMessage());
            return List.of();
        }
    }

    // La IA puede repetir o inventar ids: nos quedamos solo con candidatos reales, sin duplicados.
    private static List<UUID> onlyCandidates(List<UUID> ids, Set<UUID> candidateIds, int size) {
        return ids.stream().filter(Objects::nonNull).filter(candidateIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().limit(size).toList();
    }

    // Fallback sin IA: mas categorias compartidas con los favoritos primero; sin coincidencias no se sugiere.
    private static List<UUID> rankByCategories(List<RecommendationCandidate> favorites,
            List<RecommendationCandidate> candidates, int size) {
        Map<UUID, Long> categoryWeight = favorites.stream().flatMap(f -> f.categoryIds().stream())
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        record Scored(RecommendationCandidate candidate, long score) {
        }
        return candidates.stream()
                .map(c -> new Scored(c, c.categoryIds().stream().mapToLong(id -> categoryWeight.getOrDefault(id, 0L)).sum()))
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingLong(Scored::score).reversed()
                        .thenComparing(s -> s.candidate().name(), String.CASE_INSENSITIVE_ORDER))
                .limit(size)
                .map(s -> s.candidate().id())
                .toList();
    }

    // Una sola carga para todos (la base es remota) y despues se respeta el orden del ranking.
    // Un producto borrado entre la consulta de candidatos y esta carga simplemente se omite.
    private List<Product> loadProducts(List<UUID> ids) {
        Map<UUID, Product> byId = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private record CachedRanking(String fingerprint, List<UUID> ids, Instant expiresAt) {
    }
}
