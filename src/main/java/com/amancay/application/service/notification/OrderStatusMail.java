package com.amancay.application.service.notification;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.amancay.application.port.out.MailContent;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

// Patron Strategy: un mail por estado del pedido. OrderStatusNotificationService elige
// cual usar segun el estado al que paso la orden; sumar un mail es sumar una subclase.
//
// Patron Template Method: build() fija la estructura comun (intro, items, totales,
// cierre) y cada subclase completa solo las partes que cambian.
public abstract class OrderStatusMail {

    public abstract OrderStatus status();

    protected abstract String subject(Order order);

    protected abstract String intro(Order order);

    protected String closing(Order order) {
        return "";
    }

    public final MailContent build(Order order, Map<UUID, OrderItemProduct> products) {
        StringBuilder body = new StringBuilder(intro(order)).append("\n\n");
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
        String closing = closing(order);
        if (!closing.isBlank()) {
            body.append('\n').append(closing).append('\n');
        }
        return new MailContent(subject(order), body.toString());
    }

    private static String money(BigDecimal amount) {
        return amount.toPlainString();
    }
}
