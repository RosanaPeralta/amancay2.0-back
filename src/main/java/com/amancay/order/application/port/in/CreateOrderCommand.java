package com.amancay.order.application.port.in;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(UUID requesterId, UUID shippingAddressId, List<Item> items) {

    public CreateOrderCommand {
        if (shippingAddressId == null) {
            throw new IllegalArgumentException("shippingAddressId is required");
        }
        items = List.copyOf(items);
    }

    public record Item(UUID productVariantId, int quantity) {
        public Item {
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be greater than zero");
            }
        }
    }
}
