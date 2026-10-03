package com.amancay.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.amancay.domain.model.OrderItemProduct;

public interface LoadOrderItemProductsPort {
    // Indexado por variantId; las variantes que ya no existen quedan afuera del mapa.
    Map<UUID, OrderItemProduct> findByVariantIds(Set<UUID> variantIds);
}
