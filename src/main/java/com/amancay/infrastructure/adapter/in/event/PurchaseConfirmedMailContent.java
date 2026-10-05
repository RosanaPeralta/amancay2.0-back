package com.amancay.infrastructure.adapter.in.event;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;

@Component
public class PurchaseConfirmedMailContent {

    public MailContent build(Order order, Map<UUID, OrderItemProduct> products) {
        return new MailContent(subject(order), body(order, products));
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
