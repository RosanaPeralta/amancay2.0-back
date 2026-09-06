package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    Page<Product> findByActive(boolean active, Pageable pageable);

        @Query("""
            SELECT p
            FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
        Page<Product> searchByNameOrSlug(
            @Param("search") String search,
            Pageable pageable);

        @Query("""
            SELECT p
            FROM Product p
            WHERE p.active = :active
              AND (
              LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
        Page<Product> searchByActive(
            @Param("active") boolean active,
            @Param("search") String search,
            Pageable pageable);
}