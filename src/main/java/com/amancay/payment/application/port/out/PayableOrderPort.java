package com.amancay.payment.application.port.out;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Lo que el modulo de pagos necesita de una orden. El adaptador lo resuelve contra
// los puertos de entrada del modulo order, asi payment no depende de su modelo.
public interface PayableOrderPort {

    // Valida que el requester pueda ver la orden (lanza 403/404 si no).
    PayableOrder loadForRequester(UUID requesterId, UUID orderId);

    // Sin chequeo de permisos: para el admin confirmando un pago.
    PayableOrder load(UUID orderId);

    List<PayableOrder> loadAll(Collection<UUID> orderIds);

    // El pago quedo aprobado: la orden pasa a preparacion.
    void markAsPaid(UUID orderId);

    record PayableOrder(UUID id, UUID buyerId, BigDecimal total, List<Line> lines) {

        public record Line(UUID productVariantId, int quantity) {
        }
    }
}
