package com.amancay.application.port.in;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.OrderStatus;

// Patron Command: el pedido "avanzar esta orden a targetStatus" como objeto. Se arma
// en un lugar (el admin o la logistica), viaja por la cola y lo ejecuta otro
// (ApplyOrderStatusCommandUseCase). commandId permite reconocer reentregas.
public record AdvanceOrderStatusCommand(UUID commandId, UUID orderId, OrderStatus targetStatus, String requestedBy,
        Instant requestedAt) {
}
