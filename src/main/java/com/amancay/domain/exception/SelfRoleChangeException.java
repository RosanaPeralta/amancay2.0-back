package com.amancay.domain.exception;

public class SelfRoleChangeException extends RuntimeException {
    public SelfRoleChangeException() {
        super("An admin cannot remove their own ADMIN role");
    }
}
