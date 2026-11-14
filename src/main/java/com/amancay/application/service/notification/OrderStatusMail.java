package com.amancay.application.service.notification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amancay.application.port.out.MailContent;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.ShippingAddress;

// Patron Strategy: un mail por estado del pedido. OrderStatusNotificationService elige
// cual usar segun el estado al que paso la orden; sumar un mail es sumar una subclase.
//
// Patron Template Method: build() fija la estructura comun (intro, items, totales,
// cierre) y cada subclase completa solo las partes que cambian. Cada mail sale en HTML con
// el estilo de Amancay (OrderMailHtml) y en texto plano como alternativa.
public abstract class OrderStatusMail {

    public abstract OrderStatus status();

    protected abstract String subject(Order order);

    // El titulo grande del mail HTML.
    protected abstract String headline(Order order);

    protected abstract String intro(Order order);

    protected String closing(Order order) {
        return "";
    }

    public final MailContent build(Order order, Map<UUID, OrderItemProduct> products) {
        String intro = intro(order);
        String closing = closing(order);
        List<OrderMailHtml.Line> lines = new ArrayList<>();
        StringBuilder body = new StringBuilder(intro).append("\n\n");
        for (OrderItem item : order.getItems()) {
            OrderItemProduct product = products.get(item.productVariantId());
            String name = product == null ? item.productVariantId().toString() : product.name();
            lines.add(new OrderMailHtml.Line(name, product == null ? null : product.imageUrl(), item.quantity(),
                    item.subtotal()));
            body.append("- ").append(name).append(" x").append(item.quantity())
                    .append(" (").append(money(item.unitPrice())).append(" c/u) = ")
                    .append(money(item.subtotal())).append('\n');
        }
        body.append('\n')
                .append("Subtotal: ").append(money(order.getSubtotal())).append('\n')
                .append("Envio: ").append(money(order.getShippingCost())).append('\n')
                .append("Total: ").append(money(order.getTotal())).append('\n');
        if (!closing.isBlank()) {
            body.append('\n').append(closing).append('\n');
        }
        String html = OrderMailHtml.render(new OrderMailHtml.View(orderNumber(order), status(), headline(order), intro,
                lines, order.getSubtotal(), order.getShippingCost(), order.getTotal(), closing));
        return new MailContent(subject(order), body.toString(), html);
    }

    // El numero corto que muestra el front (#2ff37c59).
    protected static String orderNumber(Order order) {
        return order.getId().toString().substring(0, 8);
    }

    // La direccion de envio en una linea, salteando las partes vacias.
    protected static String shippingAddress(Order order) {
        ShippingAddress address = order.getShippingAddress();
        if (address == null) {
            return "la dirección que elegiste al comprar.";
        }
        String street = Stream.of(address.street(), address.number(), address.floorApt())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return Stream.of(street, address.city(), address.province(), address.postalCode(), address.country())
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String money(BigDecimal amount) {
        return amount.toPlainString();
    }
}
