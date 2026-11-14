package com.amancay.application.service.notification;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.ShippingAddress;

// El pedido salio del deposito: "tu pedido esta en camino".
@Component
public class OrderDispatchedMail extends OrderStatusMail {

    @Override
    public OrderStatus status() {
        return OrderStatus.DESPACHADO;
    }

    @Override
    protected String subject(Order order) {
        return "Tu pedido #" + order.getId() + " esta en camino";
    }

    @Override
    protected String intro(Order order) {
        return "Tu pedido ya fue despachado y esta en camino a:\n" + format(order.getShippingAddress());
    }

    @Override
    protected String closing(Order order) {
        return "Gracias por comprar en Amancay.";
    }

    private static String format(ShippingAddress address) {
        if (address == null) {
            return "la direccion que elegiste al comprar.";
        }
        String street = Stream.of(address.street(), address.number(), address.floorApt())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return Stream.of(street, address.city(), address.province(), address.postalCode(), address.country())
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));
    }
}
