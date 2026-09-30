package com.amancay.exceptions;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(UUID id) {
        super("Review not found: " + id);
    }
}
