package com.amancay.infrastructure.adapters.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.domain.ports.out.ReviewPurchasePort;
import com.amancay.service.PurchaseVerifier;

@Component
public class ReviewPurchaseAdapter implements ReviewPurchasePort {
    private final PurchaseVerifier purchaseVerifier;

    public ReviewPurchaseAdapter(PurchaseVerifier purchaseVerifier) {
        this.purchaseVerifier = purchaseVerifier;
    }

    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return purchaseVerifier.hasPurchased(userId, productId);
    }
}