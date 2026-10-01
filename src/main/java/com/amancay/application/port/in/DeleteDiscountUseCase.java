package com.amancay.application.port.in;

public interface DeleteDiscountUseCase {
    // Falla si algun producto todavia lo tiene asignado.
    void delete(Long id);
}
