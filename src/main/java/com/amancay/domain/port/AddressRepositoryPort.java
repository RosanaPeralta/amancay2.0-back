package com.amancay.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Address;

public interface AddressRepositoryPort {
    Optional<Address> findById(UUID id);

    // Mas viejas primero.
    List<Address> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    // Desmarca la predeterminada actual con un UPDATE directo, para que el indice unico
    // parcial de la base (uq_addresses_default_per_user) nunca vea dos en true a la vez.
    void clearDefaultByUserId(UUID userId);

    Address save(Address address);

    void delete(Address address);
}
