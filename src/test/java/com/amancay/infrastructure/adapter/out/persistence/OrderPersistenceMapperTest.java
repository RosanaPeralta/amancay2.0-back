package com.amancay.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderStatus;

class OrderPersistenceMapperTest {

    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();

    @Test
    void newEntityCopiesEverythingAndLinksChildrenBackToTheOrder() {
        UUID variantId = UUID.randomUUID();
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), OrderFixtures.ADDRESS,
                List.of(OrderItem.create(variantId, 2, new BigDecimal("50.00"))));

        OrderJpaEntity entity = mapper.toNewEntity(order);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getUserId()).isEqualTo(order.getUserId());
        assertThat(entity.getTotal()).isEqualByComparingTo("100.00");
        assertThat(entity.getShippingAddress().getStreet()).isEqualTo("Calle");
        assertThat(entity.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductVariantId()).isEqualTo(variantId);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getOrder()).isSameAs(entity);
        });
        assertThat(entity.getStatusHistory()).isEmpty();
    }

    @Test
    void applyChangesCopiesStatusAndAppendsOnlyNewHistoryEntries() {
        OrderJpaEntity entity = persistedEntity(OrderStatus.CREADO);
        Order order = mapper.toDomain(entity);
        order.changeStatus(OrderStatus.EN_PREPARACION);
        mapper.applyChanges(order, entity);
        entity.getStatusHistory().getFirst().setId(UUID.randomUUID());

        Order reloaded = mapper.toDomain(entity);
        reloaded.changeStatus(OrderStatus.DESPACHADO);
        mapper.applyChanges(reloaded, entity);

        assertThat(entity.getStatus()).isEqualTo(OrderStatus.DESPACHADO);
        assertThat(entity.getStatusHistory()).extracting(OrderStatusHistoryJpaEntity::getStatus)
                .containsExactly(OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO);
        assertThat(entity.getStatusHistory()).allSatisfy(history -> assertThat(history.getOrder()).isSameAs(entity));
    }

    @Test
    void toDomainRoundTripsPersistedData() {
        OrderJpaEntity entity = persistedEntity(OrderStatus.DESPACHADO);

        Order order = mapper.toDomain(entity);

        assertThat(order.getId()).isEqualTo(entity.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DESPACHADO);
        assertThat(order.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(order.getShippingAddress()).isEqualTo(OrderFixtures.ADDRESS);
        assertThat(order.getItems()).singleElement()
                .satisfies(item -> assertThat(item.id()).isEqualTo(entity.getItems().getFirst().getId()));
    }

    private OrderJpaEntity persistedEntity(OrderStatus status) {
        OrderJpaEntity entity = mapper.toNewEntity(OrderFixtures.newOrder());
        entity.setId(UUID.randomUUID());
        entity.setStatus(status);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.getItems().forEach(item -> item.setId(UUID.randomUUID()));
        return entity;
    }
}
