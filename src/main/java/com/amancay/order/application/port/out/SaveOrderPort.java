package com.amancay.order.application.port.out;

import com.amancay.order.domain.model.Order;

public interface SaveOrderPort {
    // Devuelve la orden tal como quedo persistida (con ids y timestamps asignados).
    Order save(Order order);
}
