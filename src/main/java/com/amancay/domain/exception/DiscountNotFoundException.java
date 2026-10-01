package com.amancay.domain.exception;

public class DiscountNotFoundException extends RuntimeException {
    public DiscountNotFoundException(Long id) {
        super("Discount not found: " + id);
    }
}