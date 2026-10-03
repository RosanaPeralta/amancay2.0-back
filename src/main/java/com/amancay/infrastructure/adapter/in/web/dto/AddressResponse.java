package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.Address;

public record AddressResponse(
        UUID id,
        String street,
        Integer number,
        String floorApt,
        String city,
        String province,
        String country,
        String postalCode,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(address.getId(), address.getStreet(), address.getNumber(), address.getFloorApt(),
                address.getCity(), address.getProvince(), address.getCountry(), address.getPostalCode(),
                address.isDefaultAddress(), address.getCreatedAt(), address.getUpdatedAt());
    }
}
