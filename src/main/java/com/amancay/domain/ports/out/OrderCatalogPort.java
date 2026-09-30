package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.PurchaseOrder;

public interface OrderCatalogPort {
    List<PurchaseOrder> findByUserId(UUID userId);

    Optional<PurchaseOrder> findById(UUID id);

    PurchaseOrder save(PurchaseOrder order);
}