package com.amancay.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        UUID userId,
        @NotNull UUID shippingAddressId,
        @NotNull @Valid @Size(min = 1) List<ItemRequest> items) {

    public record ItemRequest(
            @NotNull UUID productVariantId,
            @NotNull Integer quantity) {
    }
}
