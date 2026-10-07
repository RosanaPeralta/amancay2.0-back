package com.amancay.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

import com.amancay.domain.model.Product;

public interface ManageProductDiscountUseCase {
    Product assignDiscount(UUID requesterId, UUID productId, Long discountId);

    // Crea el descuento y se lo asigna en la misma transaccion: si el producto no existe,
    // tampoco queda creado el descuento.
    Product createAndAssignDiscount(UUID requesterId, UUID productId, BigDecimal percentage, String description);

    Product deleteDiscount(UUID requesterId, UUID productId);
}
