package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.out.LoadRecommendationCandidatesPort;
import com.amancay.domain.model.RecommendationCandidate;
import com.amancay.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.repository.ProductSpecifications;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataCategoryRepository;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataProductRepository;

@Component
class RecommendationCandidatesAdapter implements LoadRecommendationCandidatesPort {

    // Tope de productos que viajan en el prompt; alcanza para el catalogo actual.
    private static final int MAX_CANDIDATES = 200;

    private final SpringDataProductRepository productRepository;
    private final SpringDataCategoryRepository categoryRepository;

    RecommendationCandidatesAdapter(SpringDataProductRepository productRepository,
            SpringDataCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationCandidate> findActive() {
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(CategoryJpaEntity::getId, CategoryJpaEntity::getName));
        return productRepository.findAll(ProductSpecifications.matching(null, null, true),
                PageRequest.of(0, MAX_CANDIDATES, Sort.by("name"))).stream()
                .map(product -> toCandidate(product, categoryNames))
                .toList();
    }

    private static RecommendationCandidate toCandidate(ProductJpaEntity product, Map<UUID, String> categoryNames) {
        List<String> names = product.getCategoryIds().stream().map(categoryNames::get).filter(Objects::nonNull)
                .sorted().toList();
        return new RecommendationCandidate(product.getId(), product.getName(), product.getShortDescription(),
                product.getCategoryIds(), names);
    }
}
