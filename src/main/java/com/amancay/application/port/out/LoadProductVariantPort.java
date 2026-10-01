package com.amancay.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// Lo unico que hace falta saber de una variante para armar un pedido.
public interface LoadProductVariantPort {
    Optional<ProductVariantInfo> findById(UUID id);

    record ProductVariantInfo(UUID id, BigDecimal price, int stockQuantity) {
    }
}
