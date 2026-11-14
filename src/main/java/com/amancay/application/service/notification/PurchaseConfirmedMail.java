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
        return "Confirmamos tu compra #" + order.getId();
    }

    @Override
    protected String intro(Order order) {
        return "Tu compra se realizo con exito y ya esta en preparacion.";
    }
}
