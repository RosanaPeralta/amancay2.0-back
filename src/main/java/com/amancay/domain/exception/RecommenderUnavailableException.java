package com.amancay.domain.exception;

// La IA no esta configurada o fallo; el caso de uso la atrapa y cae al fallback.
public class RecommenderUnavailableException extends RuntimeException {
    public RecommenderUnavailableException(String message) {
        super(message);
    }

    public RecommenderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
