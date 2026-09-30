package com.amancay.order.application.port.in;

import java.util.UUID;

import com.amancay.order.domain.model.Order;
import com.amancay.order.domain.model.OrderStatus;

// Unico punto de entrada para mover el estado de un pedido: lo usa tanto una
// accion sincrona (admin marca "despachado" desde el panel) como otro modulo
// (payment, cuando un pago queda aprobado) o, mas adelante, un consumer de la
// cola de mensajes. La validacion de la transicion vive en OrderStatus (State).
public interface ChangeOrderStatusUseCase {
    Order changeStatus(UUID orderId, OrderStatus newStatus);
}
