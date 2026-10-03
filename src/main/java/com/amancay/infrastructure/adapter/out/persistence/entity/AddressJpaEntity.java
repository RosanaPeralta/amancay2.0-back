package com.amancay.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// El id lo asigna el dominio (Address.create), por eso no hay @GeneratedValue.
@Entity
@Table(name = "addresses")
@Getter
@Setter
public class AddressJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String street;

    @Column(nullable = false)
    private Integer number;

    @Column(name = "floor_apt")
    private String floorApt;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 120)
    private String province;

    @Column(nullable = false, length = 120)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
