package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.infrastructure.adapter.out.persistence.entity.ProductVariantJpaEntity;

public interface SpringDataProductVariantRepository extends JpaRepository<ProductVariantJpaEntity, UUID> {

    @Modifying
    @Query("update ProductVariantJpaEntity v set v.stockQuantity = v.stockQuantity - :quantity "
            + "where v.id = :id and v.stockQuantity >= :quantity")
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);

    @Query("select v from ProductVariantJpaEntity v join fetch v.product p left join fetch p.images where v.id in :ids")
    List<ProductVariantJpaEntity> findWithProductByIdIn(@Param("ids") Collection<UUID> ids);
}
