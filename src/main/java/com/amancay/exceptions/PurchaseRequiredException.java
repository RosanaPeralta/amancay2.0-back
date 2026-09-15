package com.amancay.exceptions;

import java.util.UUID;

public class PurchaseRequiredException extends RuntimeException {
    public PurchaseRequiredException(UUID productId) {
        super("A purchase of product " + productId + " is required to review it");
    }
}
