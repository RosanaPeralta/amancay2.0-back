package com.amancay.domain.exception;

import java.util.UUID;

public class FavoriteNotFoundException extends RuntimeException {
    public FavoriteNotFoundException(UUID productId) {
        super("Product " + productId + " is not a favorite of this user");
    }
}
