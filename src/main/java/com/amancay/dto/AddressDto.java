package com.amancay.dto;

import java.time.Instant;
import java.util.UUID;

public record AddressDto(
        UUID id,
        String street,
        String number,
        String floorApt,
        String city,
        String province,
        String country,
        String postalCode,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt) {
}
