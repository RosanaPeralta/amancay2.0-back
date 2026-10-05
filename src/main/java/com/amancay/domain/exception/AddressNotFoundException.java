package com.amancay.domain.exception;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(UUID id) {
        super("Address not found: " + id);
    }
}
