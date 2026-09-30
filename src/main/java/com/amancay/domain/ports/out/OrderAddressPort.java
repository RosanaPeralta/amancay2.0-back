package com.amancay.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.CustomerAddress;

public interface OrderAddressPort {
    Optional<CustomerAddress> findAddressById(UUID id);
}