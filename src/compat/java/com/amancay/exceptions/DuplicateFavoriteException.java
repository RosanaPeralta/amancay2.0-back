package com.amancay.exceptions;

import java.util.UUID;

public class DuplicateFavoriteException extends RuntimeException {
    public DuplicateFavoriteException(UUID productId) {
        super("Product " + productId + " is already a favorite of this user");
    }
}
