package com.amancay.order.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// Lo unico que el modulo de ordenes necesita saber de una variante para armar un pedido.
public interface LoadProductVariantPort {
    Optional<ProductVariantInfo> findById(UUID id);

    record ProductVariantInfo(UUID id, BigDecimal price, int stockQuantity) {
    }
}
