package com.amancay.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

class PurchaseConfirmedMailContentTest {

    private final PurchaseConfirmedMailContent mailContent = new PurchaseConfirmedMailContent();

    @Test
    void subjectNamesTheOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.EN_PREPARACION);

        MailContent content = mailContent.build(order, Map.of());

        assertThat(content.subject()).contains(orderId.toString());
    }

    @Test
    void bodyListsEachItemWithItsProductNameAndTheOrderTotals() {
        UUID variantId = UUID.randomUUID();
        OrderItem item = new OrderItem(UUID.randomUUID(), variantId, 2, new BigDecimal("50.00"));
        Order order = OrderFixtures.persisted(UUID.randomUUID(), UUID.randomUUID(), OrderStatus.EN_PREPARACION,
                new BigDecimal("100.00"), List.of(item));
        Map<UUID, OrderItemProduct> products = Map.of(variantId,
                new OrderItemProduct(variantId, UUID.randomUUID(), "Maceta de barro", null));

        MailContent content = mailContent.build(order, products);

        assertThat(content.body()).contains("Maceta de barro", "x2", "50.00 c/u", "Total: 100.00");
    }

    @Test
    void fallsBackToTheVariantIdWhenTheProductIsNoLongerAvailable() {
        UUID variantId = UUID.randomUUID();
        OrderItem item = new OrderItem(UUID.randomUUID(), variantId, 1, new BigDecimal("10.00"));
        Order order = OrderFixtures.persisted(UUID.randomUUID(), UUID.randomUUID(), OrderStatus.EN_PREPARACION,
                new BigDecimal("10.00"), List.of(item));

        MailContent content = mailContent.build(order, Map.of());

        assertThat(content.body()).contains(variantId.toString());
    }
}
