package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.infrastructure.adapter.out.persistence.entity.FavoriteJpaEntity;

public interface SpringDataFavoriteRepository extends JpaRepository<FavoriteJpaEntity, UUID> {
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<FavoriteJpaEntity> findByUserIdAndProductId(UUID userId, UUID productId);

    Page<FavoriteJpaEntity> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT f.productId FROM FavoriteJpaEntity f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<UUID> findProductIdsByUserId(@Param("userId") UUID userId);
}
