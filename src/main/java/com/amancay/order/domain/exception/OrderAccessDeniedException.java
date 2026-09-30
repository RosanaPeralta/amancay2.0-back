package com.amancay.order.domain.exception;

// Reemplaza al AccessDeniedException de Spring Security dentro del modulo: el
// dominio no depende del framework. ApiExceptionHandler la traduce a 403.
public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(String message) {
        super(message);
    }
}
