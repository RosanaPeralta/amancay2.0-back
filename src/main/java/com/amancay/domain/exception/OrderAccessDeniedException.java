package com.amancay.domain.exception;

// ApiExceptionHandler la traduce a 403.
public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(String message) {
        super(message);
    }
}
