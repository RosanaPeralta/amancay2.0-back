package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Address;

public interface SetDefaultAddressUseCase {
    Address setDefault(UUID userId, UUID addressId);
}
