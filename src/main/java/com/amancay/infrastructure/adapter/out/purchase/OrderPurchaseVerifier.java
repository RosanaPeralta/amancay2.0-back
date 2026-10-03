package com.amancay.infrastructure.adapter.out.purchase;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PurchaseVerifierPort;
import com.amancay.domain.model.PaymentStatus;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataOrderRepository;

/**
 * Considera que el usuario compró el producto cuando tiene un pedido que lo incluye con un
 * pago {@link PaymentStatus#APROBADO}. Un pedido creado pero sin pagar (o con el pago
 * pendiente/rechazado) no alcanza para reseñar.
 */
@Component
@ConditionalOnProperty(name = "amancay.reviews.verify-purchase", havingValue = "true", matchIfMissing = true)
public class OrderPurchaseVerifier implements PurchaseVerifierPort {

    private final SpringDataOrderRepository orderRepository;

    public OrderPurchaseVerifier(SpringDataOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return orderRepository.existsPurchase(userId, productId, PaymentStatus.APROBADO);
    }
}
