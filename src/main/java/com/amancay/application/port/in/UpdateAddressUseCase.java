package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Address;

public interface UpdateAddressUseCase {
    Address update(UUID userId, UUID addressId, AddressData data);
}
