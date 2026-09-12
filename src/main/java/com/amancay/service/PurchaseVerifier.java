package com.amancay.service;

import java.util.UUID;

public interface PurchaseVerifier {
    boolean hasPurchased(UUID userId, UUID productId);
}
