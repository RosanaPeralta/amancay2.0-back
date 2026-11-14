package com.amancay.application.service.notification;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// El pago se aprobo y el pedido paso a preparacion.
@Component
public class PurchaseConfirmedMail extends OrderStatusMail {

    @Override
    public OrderStatus status() {
        return OrderStatus.EN_PREPARACION;
    }

    @Override
    protected String subject(Order order) {
        return "Confirmamos tu compra #" + orderNumber(order);
    }

    @Override
    protected String headline(Order order) {
        return "¡Gracias por tu compra!";
    }

    @Override
    protected String intro(Order order) {
        return "Tu pago se aprobó y ya estamos preparando tu pedido. Te avisamos cuando salga del depósito.";
    }
}
