package com.amancay.domain.ports.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.CustomerAddress;

public interface AddressUseCases {
    List<CustomerAddress> list(UUID userId);

    CustomerAddress create(UUID userId, AddressData data);

    CustomerAddress update(UUID userId, UUID addressId, AddressData data);

    void delete(UUID userId, UUID addressId);

    CustomerAddress setDefault(UUID userId, UUID addressId);

    record AddressData(String street, Integer number, String floorApt, String city, String province, String country,
            String postalCode) {
    }
}