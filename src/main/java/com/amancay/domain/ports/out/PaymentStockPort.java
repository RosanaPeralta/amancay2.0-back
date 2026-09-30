package com.amancay.domain.ports.out;

import java.util.UUID;

public interface PaymentStockPort {
    boolean decrement(UUID variantId, int quantity);
}