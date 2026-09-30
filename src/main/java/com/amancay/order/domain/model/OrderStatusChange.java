package com.amancay.order.domain.model;

import java.time.Instant;
import java.util.UUID;

// Entrada del historial de estados. id y changedAt quedan en null hasta que se
// persiste: los asigna la base, y el adaptador usa el id null para saber que
// entradas son nuevas.
public record OrderStatusChange(UUID id, OrderStatus status, Instant changedAt) {

    public static OrderStatusChange of(OrderStatus status) {
        return new OrderStatusChange(null, status, null);
    }

    public boolean isNew() {
        return id == null;
    }
}
