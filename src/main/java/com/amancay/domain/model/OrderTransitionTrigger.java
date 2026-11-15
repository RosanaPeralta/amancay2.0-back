package com.amancay.domain.model;

// Quien puede hacer avanzar un pedido desde cada estado (ver OrderStatus).
public enum OrderTransitionTrigger {
    // La app, al aprobarse el pago: CREADO -> EN_PREPARACION.
    PAYMENT,
    // La logistica externa, por RabbitMQ. Ida: EN_PREPARACION -> LISTO_PARA_DESPACHAR -> DESPACHADO
    // -> ENTREGADO. Vuelta: ESPERANDO_RETIRO -> RETIRADO -> DEVUELTO.
    LOGISTICS,
    // La app, al aprobarse una devolucion: ENTREGADO -> ESPERANDO_RETIRO.
    RETURN_REQUEST
}
