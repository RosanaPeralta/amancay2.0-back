package com.amancay.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementación temporal de {@link PurchaseVerifier} que autoriza a cada usuario, utilizada
 * mientras el módulo de Órdenes aún no existe y no hay historial de compras para consultar.
 *
 * <p>Solo se registra cuando {@code amancay.reviews.verify-purchase} está ausente
 * o es {@code false}. Una vez que llegue una implementación real respaldada por órdenes,
 * configurar {@code amancay.reviews.verify-purchase=true} deshabilita este bean y
 * el real toma el control: no se requiere ningún cambio de código aquí.
 */
@Service
@ConditionalOnProperty(name = "amancay.reviews.verify-purchase", havingValue = "false", matchIfMissing = true)
public class AlwaysAllowPurchaseVerifier implements PurchaseVerifier {
    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return true;
    }
}
