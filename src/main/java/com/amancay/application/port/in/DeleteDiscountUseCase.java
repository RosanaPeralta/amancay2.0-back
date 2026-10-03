package com.amancay.application.port.in;

import java.util.UUID;

public interface DeleteDiscountUseCase {
    // Falla si algun producto todavia lo tiene asignado.
    void delete(UUID requesterId, Long id);
}
