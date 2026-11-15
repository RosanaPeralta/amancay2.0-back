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

class OrderDeliveredMailTest {

    private final OrderDeliveredMail mail = new OrderDeliveredMail();

    @Test
    void isTheMailForDeliveredOrders() {
        assertThat(mail.status()).isEqualTo(OrderStatus.ENTREGADO);
    }

    @Test
    void tellsTheBuyerWhereItWasDeliveredAndHowLongTheyHaveToReturnIt() {
        UUID orderId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ShippingAddress address = new ShippingAddress("Av. Corrientes", "1234", null, "CABA", "Buenos Aires",
                "Argentina", "C1043");
        Order order = new Order(orderId, UUID.randomUUID(), UUID.randomUUID(), address, OrderStatus.ENTREGADO,
                new BigDecimal("100.00"), new BigDecimal("12.50"), new BigDecimal("112.50"), Instant.now(),
                Instant.now(), List.of(new OrderItem(UUID.randomUUID(), variantId, 2, new BigDecimal("50.00"))),
                List.of());

        MailContent content = mail.build(order, Map.of(variantId,
                new OrderItemProduct(variantId, UUID.randomUUID(), "Maceta de barro", null)));

        assertThat(content.subject()).contains(orderId.toString().substring(0, 8), "entregado");
        assertThat(content.body()).contains("Entregamos tu pedido en", "Av. Corrientes 1234, CABA, Buenos Aires, C1043",
                "Maceta de barro x2", "Total: 112.50", "10 días para pedir la devolución");
    }
}
