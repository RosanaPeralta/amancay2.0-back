package com.amancay.application.port.out;

import java.util.UUID;

// Si el usuario compro el producto (requisito para resenarlo).
public interface PurchaseVerifierPort {
    boolean hasPurchased(UUID userId, UUID productId);
}
