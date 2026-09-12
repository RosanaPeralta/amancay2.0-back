package com.amancay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank @Size(max = 255) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 50) String floorApt,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 120) String province,
        @NotBlank @Size(max = 120) String country,
        @Size(max = 20) String postalCode) {
}
