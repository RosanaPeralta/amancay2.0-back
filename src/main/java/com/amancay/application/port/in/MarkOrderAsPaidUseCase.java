package com.amancay.application.port.in;

import java.util.UUID;

// Lo usa el cobro cuando un pago queda aprobado: pasa la orden a EN_PREPARACION sin pedir
// rol ADMIN, porque no lo dispara un usuario sino el propio sistema. Ningun controller
// lo expone.
public interface MarkOrderAsPaidUseCase {
    void markAsPaid(UUID orderId);
}
