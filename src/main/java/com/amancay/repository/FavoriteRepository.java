package com.amancay.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId);

    Page<Favorite> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT f.productId FROM Favorite f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<UUID> findProductIdsByUserId(@Param("userId") UUID userId);
}
