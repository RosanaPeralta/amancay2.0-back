package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// Unico punto de entrada para cambiar el estado de un pedido.
public interface ChangeOrderStatusUseCase {
    Order changeStatus(UUID requesterId, UUID orderId, OrderStatus newStatus);
}
