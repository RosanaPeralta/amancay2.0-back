package com.amancay.domain.exception;

// La accion es solo para ADMIN y quien la pidio no lo es. ApiExceptionHandler la traduce
// a 403 con el mismo cuerpo que un @PreAuthorize rechazado.
public class AdminRequiredException extends RuntimeException {
    public AdminRequiredException() {
        super("Admin role required");
    }
}
