package com.amancay.domain.exception;

import java.util.UUID;

public class BuyerEmailNotFoundException extends RuntimeException {
    public BuyerEmailNotFoundException(UUID userId) {
        super("No email on file for user: " + userId);
    }
}
