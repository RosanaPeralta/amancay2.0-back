package com.amancay.exceptions;

import java.util.UUID;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(UUID productId, UUID existingReviewId) {
        super("Product " + productId + " was already reviewed by this user: " + existingReviewId);
    }
}
