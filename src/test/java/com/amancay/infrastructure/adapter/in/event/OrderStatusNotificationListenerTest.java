package com.amancay.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

// Sin Mockito ni Spring: cada puerto de salida se reemplaza por un fake en memoria. El
// TransactionalEventListener en si no se ejercita aca (necesitaria una transaccion real);
// lo que importa testear es la logica que dispara, no el mecanismo de Spring.
class OrderStatusNotificationListenerTest {

    private final Map<UUID, Order> orders = new HashMap<>();
    private final Map<UUID, String> emails = new HashMap<>();
    private final Map<UUID, OrderItemProduct> products = new HashMap<>();
    private final List<SentMail> sentMails = new ArrayList<>();

    private final OrderStatusNotificationListener listener = new OrderStatusNotificationListener(
            new FakeOrderRepository(),
            this::describe,
            userIds -> {
                Map<UUID, String> found = new HashMap<>();
                userIds.stream().filter(emails::containsKey).forEach(id -> found.put(id, emails.get(id)));
                return found;
            },
            (to, subject, body) -> sentMails.add(new SentMail(to, subject, body)),
            new PurchaseConfirmedMailContent());

    @Test
    void sendsThePurchaseConfirmationMailWhenTheOrderMovesToEnPreparacion() {
        UUID userId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        emails.put(userId, "buyer@amancay.com");
        products.put(variantId, new OrderItemProduct(variantId, UUID.randomUUID(), "Maceta de barro", null));
        orders.put(orderId, OrderFixtures.persisted(orderId, userId, OrderStatus.EN_PREPARACION,
                new BigDecimal("100.00"), List.of(new OrderItem(UUID.randomUUID(), variantId, 2, new BigDecimal("50.00")))));

        listener.onOrderStatusChanged(
                new OrderStatusChangedEvent(orderId, OrderStatus.CREADO, OrderStatus.EN_PREPARACION));

        assertThat(sentMails).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo("buyer@amancay.com");
            assertThat(mail.subject()).contains(orderId.toString());
            assertThat(mail.body()).contains("Maceta de barro");
        });
    }

    @Test
    void doesNotSendAnythingForOtherStatusTransitions() {
        UUID orderId = UUID.randomUUID();
        orders.put(orderId, OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.DESPACHADO));

        listener.onOrderStatusChanged(
                new OrderStatusChangedEvent(orderId, OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO));

        assertThat(sentMails).isEmpty();
    }

    @Test
    void doesNotFailWhenTheOrderNoLongerExists() {
        listener.onOrderStatusChanged(
                new OrderStatusChangedEvent(UUID.randomUUID(), OrderStatus.CREADO, OrderStatus.EN_PREPARACION));

        assertThat(sentMails).isEmpty();
    }

    @Test
    void doesNotFailWhenTheBuyerHasNoEmailOnFile() {
        UUID orderId = UUID.randomUUID();
        orders.put(orderId, OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.EN_PREPARACION));

        listener.onOrderStatusChanged(
                new OrderStatusChangedEvent(orderId, OrderStatus.CREADO, OrderStatus.EN_PREPARACION));

        assertThat(sentMails).isEmpty();
    }

    private Map<UUID, OrderItemProduct> describe(Collection<Order> requestedOrders) {
        Set<UUID> variantIds = requestedOrders.stream().flatMap(order -> order.getItems().stream())
                .map(OrderItem::productVariantId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, OrderItemProduct> result = new HashMap<>();
        variantIds.forEach(id -> {
            if (products.containsKey(id)) {
                result.put(id, products.get(id));
            }
        });
        return result;
    }

    private record SentMail(String to, String subject, String body) {
    }

    private final class FakeOrderRepository implements OrderRepositoryPort {
        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public List<Order> findByUserId(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Order> findAllById(Collection<UUID> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Order> findAllNewestFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Order save(Order order) {
            throw new UnsupportedOperationException();
        }
    }
}
