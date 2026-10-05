package com.amancay.application.port.out;

import java.util.UUID;

public interface LoadRequesterPort {
    // Si el usuario no existe, el adaptador lanza UserNotFoundException (404).
    Requester load(UUID userId);

    record Requester(UUID id, boolean admin) {
    }
}
