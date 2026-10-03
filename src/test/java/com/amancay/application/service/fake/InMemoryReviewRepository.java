package com.amancay.application.service.fake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amancay.application.port.out.ReviewRepositoryPort;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.RatingCount;
import com.amancay.domain.model.Review;
import com.amancay.domain.model.ReviewSort;
import com.amancay.domain.model.ReviewStatus;

public class InMemoryReviewRepository implements ReviewRepositoryPort {

    private final Map<UUID, Review> store = new LinkedHashMap<>();
    public final List<String> calls = new ArrayList<>();
    private Instant clock = Instant.parse("2026-01-01T00:00:00Z");

    public Review stored(UUID id) {
        return store.get(id);
    }

    public long count(String call) {
        return calls.stream().filter(call::equals).count();
    }

    @Override
    public Optional<Review> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryReviewRepository::copy);
    }

    @Override
    public Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId) {
        return store.values().stream().filter(r -> r.getProductId().equals(productId) && r.getUserId().equals(userId))
                .findFirst().map(InMemoryReviewRepository::copy);
    }

    @Override
    public boolean existsByProductIdAndUserId(UUID productId, UUID userId) {
        return findByProductIdAndUserId(productId, userId).isPresent();
    }

    @Override
    public PageResult<Review> findPublishedByProduct(UUID productId, ReviewSort sort, PageQuery page) {
        Comparator<Review> newest = Comparator.comparing(Review::getCreatedAt).reversed();
        Comparator<Review> order = switch (sort) {
            case RECENT -> newest;
            case BEST -> Comparator.comparingInt(Review::getRating).reversed().thenComparing(newest);
            case WORST -> Comparator.comparingInt(Review::getRating).thenComparing(newest);
        };
        return Pages.of(store.values().stream()
                .filter(r -> r.getProductId().equals(productId) && r.getStatus() == ReviewStatus.PUBLISHED)
                .sorted(order).map(InMemoryReviewRepository::copy).toList(), page);
    }

    @Override
    public PageResult<Review> findByUserId(UUID userId, PageQuery page) {
        return Pages.of(newestFirst(store.values().stream().filter(r -> r.getUserId().equals(userId)).toList()), page);
    }

    @Override
    public PageResult<Review> findForModeration(ReviewStatus status, UUID productId, PageQuery page) {
        return Pages.of(newestFirst(store.values().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> productId == null || r.getProductId().equals(productId)).toList()), page);
    }

    @Override
    public List<RatingCount> countPublishedGroupedByRating(UUID productId) {
        return store.values().stream()
                .filter(r -> r.getProductId().equals(productId) && r.getStatus() == ReviewStatus.PUBLISHED)
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()))
                .entrySet().stream().map(e -> new RatingCount(e.getKey(), e.getValue())).toList();
    }

    @Override
    public Review save(Review review) {
        calls.add("save");
        clock = clock.plusSeconds(1);
        Review saved = new Review(review.getId(), review.getProductId(), review.getUserId(), review.getRating(),
                review.getTitle(), review.getComment(), review.getStatus(),
                review.getCreatedAt() == null ? clock : review.getCreatedAt(), clock);
        store.put(saved.getId(), saved);
        return copy(saved);
    }

    @Override
    public void delete(Review review) {
        calls.add("delete");
        store.remove(review.getId());
    }

    private static List<Review> newestFirst(List<Review> reviews) {
        return reviews.stream().sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(InMemoryReviewRepository::copy).toList();
    }

    private static Review copy(Review r) {
        return new Review(r.getId(), r.getProductId(), r.getUserId(), r.getRating(), r.getTitle(), r.getComment(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
