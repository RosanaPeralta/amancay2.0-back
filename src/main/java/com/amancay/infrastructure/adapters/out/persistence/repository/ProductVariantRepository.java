package com.amancay.infrastructure.adapters.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.entity.ProductVariant;

@NoRepositoryBean
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    @Modifying
    @Query("update ProductVariant v set v.stockQuantity = v.stockQuantity - :quantity "
            + "where v.id = :id and v.stockQuantity >= :quantity")
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
