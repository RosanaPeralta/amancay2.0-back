package com.amancay.application.port.in;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.OrderStatus;

// Patron Command: el pedido "avanzar esta orden a targetStatus" como objeto. Lo arma la
// logistica externa, viaja por la cola y lo ejecuta el backend
// (ApplyOrderStatusCommandUseCase). commandId permite reconocer reentregas.
public record AdvanceOrderStatusCommand(UUID commandId, UUID orderId, OrderStatus targetStatus, String requestedBy,
        Instant requestedAt) {
}
