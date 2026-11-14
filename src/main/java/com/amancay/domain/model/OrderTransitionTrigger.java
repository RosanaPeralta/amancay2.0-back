package com.amancay.domain.model;

// Quien puede hacer avanzar un pedido desde cada estado (ver OrderStatus).
public enum OrderTransitionTrigger {
    // La app, al aprobarse el pago: CREADO -> EN_PREPARACION.
    PAYMENT,
    // La cola de logistica (RabbitMQ): EN_PREPARACION -> DESPACHADO -> ENTREGADO.
    FULFILLMENT,
    // La app, al aprobarse una devolucion: ENTREGADO -> DEVUELTO.
    RETURN_REQUEST
}
