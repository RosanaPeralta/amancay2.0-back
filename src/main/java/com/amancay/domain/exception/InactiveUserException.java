package com.amancay.domain.exception;

// UserRoleAuthoritiesFilter y ApiExceptionHandler la traducen a 403.
public class InactiveUserException extends RuntimeException {
    public InactiveUserException() {
        super("User is inactive");
    }
}
