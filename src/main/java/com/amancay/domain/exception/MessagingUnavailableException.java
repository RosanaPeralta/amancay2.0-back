package com.amancay.domain.exception;

public class MessagingUnavailableException extends RuntimeException {
    public MessagingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
