package com.amancay.application.service.notification;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// El pedido salio del deposito: "tu pedido esta en camino".
@Component
public class OrderDispatchedMail extends OrderStatusMail {

    @Override
    public OrderStatus status() {
        return OrderStatus.DESPACHADO;
    }

    @Override
    protected String subject(Order order) {
        return "Tu pedido #" + orderNumber(order) + " está en camino";
    }

    @Override
    protected String headline(Order order) {
        return "¡Tu pedido está en camino!";
    }

    @Override
    protected String intro(Order order) {
        return "Tu pedido ya salió del depósito y está en camino a:\n" + shippingAddress(order);
    }

    @Override
    protected String closing(Order order) {
        return "Gracias por comprar en Amancay.";
    }
}
