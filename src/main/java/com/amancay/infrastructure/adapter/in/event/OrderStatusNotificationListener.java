package com.amancay.infrastructure.adapter.in.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.amancay.application.port.in.DescribeOrderItemsQuery;
import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.application.port.out.MailSenderPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

// AFTER_COMMIT: corre recien cuando el cambio de estado ya quedo persistido (nunca antes,
// nunca si la transaccion se revierte). Es el punto de enganche para el futuro consumer de
// la cola real con reintentos: cuando exista, esta clase (u otra que llame a la misma logica)
// pasa a ser lo que la cola invoca por cada mensaje, y una excepcion aca es la senal de
// "reintentame esta entrega".
@Component
public class OrderStatusNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusNotificationListener.class);

    private final OrderRepositoryPort orderRepository;
    private final DescribeOrderItemsQuery describeOrderItems;
    private final BuyerEmailPort buyerEmailPort;
    private final MailSenderPort mailSender;

    public OrderStatusNotificationListener(OrderRepositoryPort orderRepository,
            DescribeOrderItemsQuery describeOrderItems, BuyerEmailPort buyerEmailPort, MailSenderPort mailSender) {
        this.orderRepository = orderRepository;
        this.describeOrderItems = describeOrderItems;
        this.buyerEmailPort = buyerEmailPort;
        this.mailSender = mailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} changed status: {} -> {}", event.orderId(), event.previousStatus(), event.newStatus());

        if (event.newStatus() == OrderStatus.EN_PREPARACION) {
            notifyPurchaseConfirmed(event.orderId());
        }
    }

    // El pago (tarjeta o transferencia) ya fue aprobado por quien publico el evento: este
    // metodo solo avisa, no vuelve a validar nada.
    private void notifyPurchaseConfirmed(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("No se pudo mandar el mail de compra confirmada: orden {} no encontrada", orderId);
            return;
        }
        String buyerEmail = buyerEmailPort.findEmailsByUserId(Set.of(order.getUserId())).get(order.getUserId());
        if (buyerEmail == null) {
            log.warn("No se pudo mandar el mail de compra confirmada: sin email para el usuario {}",
                    order.getUserId());
            return;
        }
        Map<UUID, OrderItemProduct> products = describeOrderItems.describe(List.of(order));
        mailSender.send(buyerEmail, subject(order), body(order, products));
    }

    private String subject(Order order) {
        return "Confirmamos tu compra #" + order.getId();
    }

    private String body(Order order, Map<UUID, OrderItemProduct> products) {
        StringBuilder body = new StringBuilder("Tu compra se realizo con exito y ya esta en preparacion.\n\n");
        for (OrderItem item : order.getItems()) {
            OrderItemProduct product = products.get(item.productVariantId());
            String name = product == null ? item.productVariantId().toString() : product.name();
            body.append("- ").append(name).append(" x").append(item.quantity())
                    .append(" (").append(money(item.unitPrice())).append(" c/u) = ")
                    .append(money(item.subtotal())).append('\n');
        }
        body.append('\n')
                .append("Subtotal: ").append(money(order.getSubtotal())).append('\n')
                .append("Envio: ").append(money(order.getShippingCost())).append('\n')
                .append("Total: ").append(money(order.getTotal())).append('\n');
        return body.toString();
    }

    private String money(BigDecimal amount) {
        return amount.toPlainString();
    }
}
