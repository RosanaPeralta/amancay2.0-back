package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.ReviewRepositoryPort;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.RatingCount;
import com.amancay.domain.model.Review;
import com.amancay.domain.model.ReviewSort;
import com.amancay.domain.model.ReviewStatus;
import com.amancay.infrastructure.adapter.out.persistence.entity.ReviewJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.PageMapper;
import com.amancay.infrastructure.adapter.out.persistence.mapper.ReviewPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.ReviewSpecifications;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataReviewRepository;

@Component
class ReviewRepositoryAdapter implements ReviewRepositoryPort {

    private final SpringDataReviewRepository repository;
    private final ReviewPersistenceMapper mapper;

    ReviewRepositoryAdapter(SpringDataReviewRepository repository, ReviewPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Review> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId) {
        return repository.findByProductIdAndUserId(productId, userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByProductIdAndUserId(UUID productId, UUID userId) {
        return repository.existsByProductIdAndUserId(productId, userId);
    }

    @Override
    public PageResult<Review> findPublishedByProduct(UUID productId, ReviewSort sort, PageQuery page) {
        return PageMapper.toPageResult(repository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED,
                PageMapper.toPageable(page, toSort(sort))), mapper::toDomain);
    }

    @Override
    public PageResult<Review> findByUserId(UUID userId, PageQuery page) {
        return PageMapper.toPageResult(
                repository.findByUserId(userId, PageMapper.toPageable(page, PageMapper.NEWEST_FIRST)),
                mapper::toDomain);
    }

    @Override
    public PageResult<Review> findForModeration(ReviewStatus status, UUID productId, PageQuery page) {
        return PageMapper.toPageResult(repository.findAll(ReviewSpecifications.matching(status, productId),
                PageMapper.toPageable(page, PageMapper.NEWEST_FIRST)), mapper::toDomain);
    }

    @Override
    public List<RatingCount> countPublishedGroupedByRating(UUID productId) {
        return repository.countPublishedGroupedByRating(productId);
    }

    @Override
    public Review save(Review review) {
        ReviewJpaEntity entity = repository.findById(review.getId()).orElseGet(ReviewJpaEntity::new);
        mapper.copyToEntity(review, entity);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public void delete(Review review) {
        repository.deleteById(review.getId());
    }

    private static Sort toSort(ReviewSort sort) {
        return switch (sort) {
            case RECENT -> Sort.by("createdAt").descending();
            case BEST -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
            case WORST -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"));
        };
    }
}
