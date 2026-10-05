package com.amancay.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// La relacion inversa con productos (para saber si esta en uso) se resuelve con
// SpringDataProductRepository.existsByDiscountId, sin mapear la coleccion aca.
@Entity
@Table(name = "discounts")
@Getter
@Setter
public class DiscountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal percentage;

    @Column(nullable = false, length = 255)
    private String description;
}
