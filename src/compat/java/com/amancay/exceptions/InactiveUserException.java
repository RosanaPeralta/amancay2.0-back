package com.amancay.exceptions;

import org.springframework.security.access.AccessDeniedException;

public class InactiveUserException extends AccessDeniedException {
    public InactiveUserException() {
        super("User is inactive");
    }
}
