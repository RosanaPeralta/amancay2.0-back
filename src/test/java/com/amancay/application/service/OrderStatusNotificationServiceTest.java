package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.amancay.application.service.fake.InMemoryOrderRepository;
import com.amancay.application.service.notification.OrderDispatchedMail;
import com.amancay.application.service.notification.PurchaseConfirmedMail;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.BuyerEmailNotFoundException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

class OrderStatusNotificationServiceTest {

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final Map<UUID, String> emails = new HashMap<>();
    private final Map<UUID, OrderItemProduct> products = new HashMap<>();
    private final List<SentMail> sentMails = new ArrayList<>();

    private final OrderStatusNotificationService service = new OrderStatusNotificationService(
            List.of(new PurchaseConfirmedMail(), new OrderDispatchedMail()),
            orders,
            this::describe,
            userIds -> userIds.stream().filter(emails::containsKey)
                    .collect(Collectors.toMap(id -> id, emails::get)),
            (to, subject, body) -> sentMails.add(new SentMail(to, subject, body)));

    @Test
    void sendsThePurchaseConfirmationWhenTheOrderMovesToPreparation() {
        UUID orderId = orderWithProduct(OrderStatus.EN_PREPARACION, "buyer@amancay.com", "Maceta de barro");

        boolean sent = service.notify(event(orderId, OrderStatus.CREADO, OrderStatus.EN_PREPARACION));

        assertThat(sent).isTrue();
        assertThat(sentMails).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo("buyer@amancay.com");
            assertThat(mail.subject()).contains("Confirmamos tu compra", orderId.toString());
            assertThat(mail.body()).contains("Maceta de barro");
        });
    }

    @Test
    void sendsTheOnItsWayMailWhenTheOrderIsDispatched() {
        UUID orderId = orderWithProduct(OrderStatus.DESPACHADO, "buyer@amancay.com", "Maceta de barro");

        boolean sent = service.notify(event(orderId, OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO));

        assertThat(sent).isTrue();
        assertThat(sentMails).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo("buyer@amancay.com");
            assertThat(mail.subject()).contains("en camino");
            assertThat(mail.body()).contains("Maceta de barro");
        });
    }

    @Test
    void doesNotSendAnythingForStatusesWithoutAMail() {
        UUID orderId = UUID.randomUUID();

        boolean sent = service.notify(event(orderId, OrderStatus.DESPACHADO, OrderStatus.ENTREGADO));

        assertThat(sent).isFalse();
        assertThat(sentMails).isEmpty();
    }

    @Test
    void throwsWhenTheOrderNoLongerExists() {
        assertThatThrownBy(() -> service.notify(
                event(UUID.randomUUID(), OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO)))
                .isInstanceOf(OrderNotFoundException.class);
        assertThat(sentMails).isEmpty();
    }

    @Test
    void throwsWhenTheBuyerHasNoEmailOnFile() {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.DESPACHADO));

        assertThatThrownBy(() -> service.notify(event(orderId, OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO)))
                .isInstanceOf(BuyerEmailNotFoundException.class);
        assertThat(sentMails).isEmpty();
    }

    @Test
    void twoMailsForTheSameStatusAreAConfigurationError() {
        assertThatThrownBy(() -> new OrderStatusNotificationService(
                List.of(new OrderDispatchedMail(), new OrderDispatchedMail()), orders, this::describe,
                userIds -> Map.of(), (to, subject, body) -> {
                }))
                .isInstanceOf(IllegalStateException.class);
    }

    private UUID orderWithProduct(OrderStatus status, String email, String productName) {
        UUID buyerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        emails.put(buyerId, email);
        products.put(variantId, new OrderItemProduct(variantId, UUID.randomUUID(), productName, null));
        orders.put(OrderFixtures.persisted(orderId, buyerId, status, new BigDecimal("100.00"),
                List.of(new OrderItem(UUID.randomUUID(), variantId, 2, new BigDecimal("50.00")))));
        return orderId;
    }

    private static OrderStatusChangedEvent event(UUID orderId, OrderStatus previous, OrderStatus next) {
        return new OrderStatusChangedEvent(UUID.randomUUID(), orderId, null, previous, next, Instant.now());
    }

    private Map<UUID, OrderItemProduct> describe(Collection<Order> requestedOrders) {
        return requestedOrders.stream().flatMap(order -> order.getItems().stream())
                .map(OrderItem::productVariantId)
                .filter(products::containsKey)
                .distinct()
                .collect(Collectors.toMap(id -> id, products::get));
    }

    private record SentMail(String to, String subject, String body) {
    }
}
