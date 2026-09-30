package com.amancay.domain.ports.out;

import java.util.UUID;

public interface ReviewPurchasePort {
    boolean hasPurchased(UUID userId, UUID productId);
}