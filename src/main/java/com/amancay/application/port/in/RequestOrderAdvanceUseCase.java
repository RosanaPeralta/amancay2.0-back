package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.OrderStatus;

// El admin pide avanzar un pedido en la logistica (EN_PREPARACION -> DESPACHADO ->
// ENTREGADO). No cambia el estado: encola el comando y lo aplica el consumidor.
public interface RequestOrderAdvanceUseCase {
    AdvanceOrderStatusCommand requestAdvance(UUID requesterId, UUID orderId, OrderStatus targetStatus);
}
