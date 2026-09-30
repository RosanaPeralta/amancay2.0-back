package com.amancay.payment.application.port.out;

import java.util.UUID;

public interface StockPort {
    // Descuenta de forma atomica; devuelve false si no habia stock suficiente.
    boolean decrement(UUID productVariantId, int quantity);
}
