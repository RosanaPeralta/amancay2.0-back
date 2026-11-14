package com.amancay.domain.exception;

// El servicio externo no pudo ubicar la direccion o no respondio.
public class ShippingQuoteUnavailableException extends RuntimeException {
    public ShippingQuoteUnavailableException(String message) {
        super(message);
    }

    public ShippingQuoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
