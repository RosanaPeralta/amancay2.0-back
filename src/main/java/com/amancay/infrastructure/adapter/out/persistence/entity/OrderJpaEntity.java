package com.amancay.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.amancay.domain.model.OrderStatus;
import com.amancay.infrastructure.adapter.out.persistence.mapper.OrderPersistenceMapper;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    // Sin relacion JPA a User: se referencia solo por id, como indica el
    // documento de requerimientos.
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "shipping_address_id")
    private UUID shippingAddressId;

    @Embedded
    private ShippingAddressEmbeddable shippingAddress;

    // orders.status es un enum nativo de Postgres (order_status), no varchar: sin el cast
    // explicito, Postgres rechaza el parametro que Hibernate manda como character varying.
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::order_status")
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREADO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    private List<OrderStatusHistoryJpaEntity> statusHistory = new ArrayList<>();

    public void addItem(OrderItemJpaEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addStatusHistory(OrderStatusHistoryJpaEntity history) {
        statusHistory.add(history);
        history.setOrder(this);
    }
}
