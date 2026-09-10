package com.amancay.service;

import java.util.UUID;

/**
 * Decides whether a user is allowed to review a product, which the business
 * rules tie to having purchased it.
 */
public interface PurchaseVerifier {
    boolean hasPurchased(UUID userId, UUID productId);
}
