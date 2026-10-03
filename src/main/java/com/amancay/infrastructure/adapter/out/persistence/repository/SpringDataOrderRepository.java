package com.amancay.infrastructure.adapter.out.persistence.repository;

import com.amancay.domain.model.PaymentStatus;
import com.amancay.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    List<OrderJpaEntity> findByUserId(UUID userId);

    List<OrderJpaEntity> findAllByOrderByCreatedAtDesc();

    // Un pedido del usuario que incluye alguna variante del producto y tiene un pago con ese estado.
    @Query("""
            SELECT COUNT(o) > 0 FROM OrderJpaEntity o JOIN o.items i
            WHERE o.userId = :userId
              AND EXISTS (SELECT 1 FROM ProductVariantJpaEntity v
                          WHERE v.id = i.productVariantId AND v.product.id = :productId)
              AND EXISTS (SELECT 1 FROM PaymentJpaEntity p
                          WHERE p.orderId = o.id AND p.status = :paymentStatus)
            """)
    boolean existsPurchase(@Param("userId") UUID userId, @Param("productId") UUID productId,
            @Param("paymentStatus") PaymentStatus paymentStatus);
}
