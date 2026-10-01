package com.amancay.infrastructure.adapter.out.purchase;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PurchaseVerifierPort;

/**
 * Implementación temporal de {@link PurchaseVerifierPort} que autoriza a cada usuario, utilizada
 * mientras no hay una verificación real contra el historial de compras.
 *
 * <p>Solo se registra cuando {@code amancay.reviews.verify-purchase} está ausente
 * o es {@code false}. Una vez que llegue una implementación real respaldada por órdenes,
 * configurar {@code amancay.reviews.verify-purchase=true} deshabilita este bean y
 * el real toma el control: no se requiere ningún cambio de código aquí.
 */
@Component
@ConditionalOnProperty(name = "amancay.reviews.verify-purchase", havingValue = "false", matchIfMissing = true)
public class AlwaysAllowPurchaseVerifier implements PurchaseVerifierPort {
    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return true;
    }
}
