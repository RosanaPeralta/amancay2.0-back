package com.amancay.order.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.amancay.order.application.port.in.CreateOrderCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull UUID shippingAddressId,
        @NotNull @Valid @Size(min = 1) List<ItemRequest> items) {

    public record ItemRequest(
            @NotNull UUID productVariantId,
            @NotNull Integer quantity) {
    }

    CreateOrderCommand toCommand(UUID requesterId) {
        return new CreateOrderCommand(requesterId, shippingAddressId, items.stream()
                .map(item -> new CreateOrderCommand.Item(item.productVariantId(), item.quantity()))
                .toList());
    }
}
