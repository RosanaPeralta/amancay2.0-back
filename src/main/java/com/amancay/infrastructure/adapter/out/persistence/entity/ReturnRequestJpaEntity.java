
package com.amancay.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.amancay.domain.model.ReturnReason;
import com.amancay.domain.model.ReturnRequestStatus;

@Entity
@Table(
    name = "return_requests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_return_requests_order_id",
            columnNames = "order_id")
    }
)
@Getter
@Setter
public class ReturnRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private OrderJpaEntity order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReturnReason reason;

    @Column(columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnRequestStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_note", columnDefinition = "text")
    private String resolutionNote;
}