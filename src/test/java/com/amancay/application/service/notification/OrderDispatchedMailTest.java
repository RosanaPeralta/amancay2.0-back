package com.amancay.application.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.MailContent;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.ShippingAddress;

class OrderDispatchedMailTest {

    private final OrderDispatchedMail mail = new OrderDispatchedMail();

    @Test
    void isTheMailForDispatchedOrders() {
        assertThat(mail.status()).isEqualTo(OrderStatus.DESPACHADO);
    }

    @Test
    void tellsTheBuyerTheOrderIsOnItsWayToTheShippingAddress() {
        UUID orderId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ShippingAddress address = new ShippingAddress("Av. Corrientes", "1234", "5B", "CABA", "Buenos Aires",
                "Argentina", "C1043");
        Order order = new Order(orderId, UUID.randomUUID(), UUID.randomUUID(), address, OrderStatus.DESPACHADO,
                new BigDecimal("100.00"), new BigDecimal("12.50"), new BigDecimal("112.50"), Instant.now(),
                Instant.now(), List.of(new OrderItem(UUID.randomUUID(), variantId, 2, new BigDecimal("50.00"))),
                List.of());

        MailContent content = mail.build(order, Map.of(variantId,
                new OrderItemProduct(variantId, UUID.randomUUID(), "Maceta de barro", null)));

        assertThat(content.subject()).contains(orderId.toString(), "en camino");
        assertThat(content.body()).contains("despachado", "Av. Corrientes 1234 5B, CABA, Buenos Aires, C1043, Argentina",
                "Maceta de barro x2", "Total: 112.50", "Gracias por comprar en Amancay.");
    }

    @Test
    void skipsMissingAddressParts() {
        ShippingAddress address = new ShippingAddress("Calle", "1", null, "Ciudad", null, "Pais", null);
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), address,
                OrderStatus.DESPACHADO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, Instant.now(), Instant.now(),
                List.of(), List.of());

        assertThat(mail.build(order, Map.of()).body()).contains("Calle 1, Ciudad, Pais\n");
    }
}
