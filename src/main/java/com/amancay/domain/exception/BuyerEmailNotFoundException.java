package com.amancay.domain.exception;

import java.util.UUID;

// El usuario existe pero no tiene email cargado: no hay a donde mandarle el mail de
// notificacion, y reintentar no lo soluciona.
public class BuyerEmailNotFoundException extends RuntimeException {
    public BuyerEmailNotFoundException(UUID userId) {
        super("No email on file for user: " + userId);
    }
}
