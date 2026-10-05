package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Address;

public interface CreateAddressUseCase {
    // La primera direccion del usuario queda como predeterminada sin que tenga que marcarla.
    Address create(UUID userId, AddressData data);
}
