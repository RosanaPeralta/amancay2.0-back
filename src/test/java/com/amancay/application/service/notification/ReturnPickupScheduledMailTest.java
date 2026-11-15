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

class ReturnPickupScheduledMailTest {

    private final ReturnPickupScheduledMail mail = new ReturnPickupScheduledMail();

    @Test
    void isTheMailForOrdersWaitingForPickup() {
        assertThat(mail.status()).isEqualTo(OrderStatus.ESPERANDO_RETIRO);
    }

    @Test
    void tellsTheBuyerWhereLogisticsWillPickUpTheReturn() {
        UUID orderId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ShippingAddress address = new ShippingAddress("Calle Falsa", "123", "2A", "Rosario", "Santa Fe",
                "Argentina", "S2000");
        Order order = new Order(orderId, UUID.randomUUID(), UUID.randomUUID(), address,
                OrderStatus.ESPERANDO_RETIRO, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"),
                Instant.now(), Instant.now(),
                List.of(new OrderItem(UUID.randomUUID(), variantId, 1, new BigDecimal("50.00"))), List.of());

        MailContent content = mail.build(order, Map.of(variantId,
                new OrderItemProduct(variantId, UUID.randomUUID(), "Maceta de barro", null)));

        assertThat(content.subject()).contains(orderId.toString().substring(0, 8), "retirar");
        assertThat(content.body()).contains("Aprobamos la devolución", "Calle Falsa 123 2A, Rosario, Santa Fe, S2000",
                "Maceta de barro x1", "vuelva a nuestro depósito");
    }
}
