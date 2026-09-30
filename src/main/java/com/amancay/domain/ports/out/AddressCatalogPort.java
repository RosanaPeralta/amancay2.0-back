package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.CustomerAddress;

public interface AddressCatalogPort {
    List<CustomerAddress> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    Optional<CustomerAddress> findByIdAndUserId(UUID addressId, UUID userId);

    CustomerAddress save(CustomerAddress address);

    void delete(CustomerAddress address);

    void clearDefaultByUserId(UUID userId);
}