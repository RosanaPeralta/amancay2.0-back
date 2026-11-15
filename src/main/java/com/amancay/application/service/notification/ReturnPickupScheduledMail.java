package com.amancay.application.service.notification;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// Se aprobo la devolucion: logistica va a pasar a buscar el pedido.
@Component
public class ReturnPickupScheduledMail extends OrderStatusMail {

    @Override
    public OrderStatus status() {
        return OrderStatus.ESPERANDO_RETIRO;
    }

    @Override
    protected String subject(Order order) {
        return "Vamos a retirar tu pedido #" + orderNumber(order);
    }

    @Override
    protected String headline(Order order) {
        return "Vamos a retirar tu devolución";
    }

    @Override
    protected String intro(Order order) {
        return "Aprobamos la devolución de tu pedido. Vamos a pasar a buscarlo por:\n" + shippingAddress(order)
                + "\n\nTené el paquete listo con todos los productos que devolvés:";
    }

    @Override
    protected String closing(Order order) {
        return "Cuando el paquete vuelva a nuestro depósito, terminamos la devolución.";
    }
}
