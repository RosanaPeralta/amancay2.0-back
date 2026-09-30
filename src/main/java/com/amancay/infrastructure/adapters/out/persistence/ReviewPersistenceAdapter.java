package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.ProductReview;
import com.amancay.domain.model.ReviewPage;
import com.amancay.domain.model.ReviewVisibility;
import com.amancay.domain.ports.in.ReviewUseCases.SortOrder;
import com.amancay.domain.ports.out.ReviewCatalogPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.ReviewPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.ProductRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.ReviewRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.ReviewSpecifications;
import com.amancay.infrastructure.adapters.out.persistence.repository.UserRepository;

@Repository
public class ReviewPersistenceAdapter implements ReviewCatalogPort {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewPersistenceAdapter(ReviewRepository reviewRepository, ProductRepository productRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean productExists(UUID productId) {
        return productRepository.existsById(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByProductAndUser(UUID productId, UUID userId) {
        return reviewRepository.existsByProductIdAndUserId(productId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findIdByProductAndUser(UUID productId, UUID userId) {
        return reviewRepository.findByProductIdAndUserId(productId, userId).map(com.amancay.entity.Review::getId);
    }

    @Override
    @Transactional
    public ProductReview save(ProductReview review) {
        com.amancay.entity.Review entity = review.id() == null ? null
                : reviewRepository.findById(review.id()).orElse(null);
        return ReviewPersistenceMapper.toDomain(reviewRepository.saveAndFlush(
                ReviewPersistenceMapper.toPersistence(review, entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductReview> findById(UUID reviewId) {
        return reviewRepository.findById(reviewId).map(ReviewPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(ProductReview review) {
        reviewRepository.deleteById(review.id());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage findByProductAndVisibility(UUID productId, ReviewVisibility visibility, int page, int size,
            List<SortOrder> sort) {
        Page<com.amancay.entity.Review> result = reviewRepository.findByProductIdAndStatus(productId,
                com.amancay.entity.ReviewStatus.valueOf(visibility.name()), pageable(page, size, sort));
        return toDomainPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage findByUser(UUID userId, int page, int size, List<SortOrder> sort) {
        return toDomainPage(reviewRepository.findByUserId(userId, pageable(page, size, sort)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage search(ReviewVisibility visibility, UUID productId, int page, int size, List<SortOrder> sort) {
        com.amancay.entity.ReviewStatus status = visibility == null ? null
                : com.amancay.entity.ReviewStatus.valueOf(visibility.name());
        return toDomainPage(reviewRepository.findAll(ReviewSpecifications.matching(status, productId),
                pageable(page, size, sort)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> countPublishedByRating(UUID productId) {
        return reviewRepository.countPublishedGroupedByRating(productId).stream()
                .collect(Collectors.toMap(com.amancay.dto.RatingCount::rating, com.amancay.dto.RatingCount::count));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findAuthorNames(Set<UUID> userIds) {
        return userRepository.findAllById(userIds).stream().filter(user -> user.getName() != null)
                .collect(Collectors.toMap(com.amancay.entity.User::getId, com.amancay.entity.User::getName,
                        (first, ignored) -> first));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findAuthorName(UUID userId) {
        return userRepository.findById(userId).map(com.amancay.entity.User::getName);
    }

    private ReviewPage toDomainPage(Page<com.amancay.entity.Review> page) {
        return new ReviewPage(page.getContent().stream().map(ReviewPersistenceMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private PageRequest pageable(int page, int size, List<SortOrder> sort) {
        List<Sort.Order> orders = sort.stream()
                .map(order -> order.ascending() ? Sort.Order.asc(order.property()) : Sort.Order.desc(order.property()))
                .toList();
        return PageRequest.of(page, size, Sort.by(orders));
    }
}