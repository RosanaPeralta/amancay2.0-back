package com.amancay.exceptions;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID productVariantId) {
        super("Insufficient stock for product variant: " + productVariantId);
    }
}
