package com.amancay.infrastructure.messaging;

import java.util.Locale;

import com.amancay.domain.model.OrderStatus;

// Nombres del exchange, colas y routing keys de pedidos. Es el contrato con cualquier
// productor o consumidor externo (por ejemplo la consola de logistica). Ver MENSAJERIA.md.
public final class RabbitMqTopology {

    // Exchange principal (topic): comandos y eventos de pedidos.
    public static final String ORDERS_EXCHANGE = "amancay.orders";

    // Dead letter exchange (topic): recibe lo que fallo, con routing key "error.<routing key original>".
    public static final String DEAD_LETTER_EXCHANGE = "amancay.orders.dlx";
    public static final String ERROR_ROUTING_KEY_PREFIX = "error.";

    // Comando "avanzar el pedido" (ver OrderTransitionTrigger.LOGISTICS).
    public static final String ADVANCE_ORDER_ROUTING_KEY = "order.command.advance";
    public static final String STATUS_COMMANDS_QUEUE = "amancay.orders.status-commands";
    public static final String STATUS_COMMANDS_DLQ = STATUS_COMMANDS_QUEUE + ".dlq";

    // Eventos "el pedido cambio de estado": order.status.<estado en minusculas>.
    public static final String STATUS_EVENT_ROUTING_KEY_PREFIX = "order.status.";
    public static final String ORDER_NOTIFICATIONS_QUEUE = "amancay.notifications.order-status";
    public static final String ORDER_NOTIFICATIONS_DLQ = ORDER_NOTIFICATIONS_QUEUE + ".dlq";

    private RabbitMqTopology() {
    }

    public static String statusEventRoutingKey(OrderStatus status) {
        return STATUS_EVENT_ROUTING_KEY_PREFIX + status.name().toLowerCase(Locale.ROOT);
    }
}
