package com.amancay.infrastructure.adapter.out.purchase;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PurchaseVerifierPort;

/** Autoriza a todos. Solo se activa con {@code amancay.reviews.verify-purchase=false}. */
@Component
@ConditionalOnProperty(name = "amancay.reviews.verify-purchase", havingValue = "false")
public class AlwaysAllowPurchaseVerifier implements PurchaseVerifierPort {
    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return true;
    }
}
