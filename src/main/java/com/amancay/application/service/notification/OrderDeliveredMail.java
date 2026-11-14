package com.amancay.application.service.notification;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// Logistica entrego el pedido. Recuerda el plazo para pedir una devolucion
// (el mismo que valida ReturnRequestService).
@Component
public class OrderDeliveredMail extends OrderStatusMail {

    @Override
    public OrderStatus status() {
        return OrderStatus.ENTREGADO;
    }

    @Override
    protected String subject(Order order) {
        return "Tu pedido #" + orderNumber(order) + " fue entregado";
    }

    @Override
    protected String headline(Order order) {
        return "¡Tu pedido llegó!";
    }

    @Override
    protected String intro(Order order) {
        return "Entregamos tu pedido en:\n" + shippingAddress(order);
    }

    @Override
    protected String closing(Order order) {
        return "Si algo no está bien, tenés 10 días para pedir la devolución desde Mis pedidos.\n"
                + "Gracias por comprar en Amancay.";
    }
}
