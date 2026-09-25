package com.amancay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @NotBlank @Size(max = 255) String street,
        @NotNull @Positive Integer number,
        @Size(max = 20) String floorApt,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 120) String province,
        @NotBlank @Size(max = 120) String country,
        @Size(max = 20) String postalCode) {
}
