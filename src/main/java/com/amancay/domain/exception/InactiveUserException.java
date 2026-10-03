package com.amancay.domain.exception;

// Antes extendia AccessDeniedException de Spring Security; ahora el dominio no depende del
// framework. UserRoleAuthoritiesFilter la traduce a 403 "User is inactive" y
// ApiExceptionHandler, si llegara a un controller, al mismo 403 "Access denied" de siempre.
public class InactiveUserException extends RuntimeException {
    public InactiveUserException() {
        super("User is inactive");
    }
}
