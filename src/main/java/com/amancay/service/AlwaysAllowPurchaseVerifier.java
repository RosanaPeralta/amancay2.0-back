package com.amancay.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Temporary {@link PurchaseVerifier} that authorises every user, used while the
 * Orders module does not exist yet and there is no purchase history to query.
 *
 * <p>It is only registered when {@code amancay.reviews.verify-purchase} is
 * absent or {@code false}. Once a real, order-backed implementation lands,
 * setting {@code amancay.reviews.verify-purchase=true} disables this bean and
 * the real one takes over: no code change is required here.
 */
@Service
@ConditionalOnProperty(name = "amancay.reviews.verify-purchase", havingValue = "false", matchIfMissing = true)
public class AlwaysAllowPurchaseVerifier implements PurchaseVerifier {
    @Override
    public boolean hasPurchased(UUID userId, UUID productId) {
        return true;
    }
}
