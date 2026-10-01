package com.amancay.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;

@Getter
public class ProductVariant {

    private final UUID id;
    private BigDecimal price;
    private int stockQuantity;

    public ProductVariant(UUID id, BigDecimal price, int stockQuantity) {
        this.id = id;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    void update(BigDecimal price, int stockQuantity) {
        this.price = price;
        this.stockQuantity = stockQuantity;
    }
}
