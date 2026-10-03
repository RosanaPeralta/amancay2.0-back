package com.amancay.infrastructure.adapter.out.purchase;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PurchaseVerifierPort;

/**
 * Implementación de {@link PurchaseVerifierPort} que autoriza a cada usuario, útil para
 * pruebas o demos sin pedidos cargados.
 *
 * <p>Solo se registra con {@code amancay.reviews.verify-purchase=false} explícito; por defecto
 * se usa {@link OrderPurchaseVerifier}, que exige una compra con pago aprobado.
 */
@Component
@ConditionalOnProperty(name = "amancay.reviews.verify-purchase", havingValue = "false")
public class AlwaysAllowPurchaseVerifier implements PurchaseVerifierPort {
    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return true;
    }
}
