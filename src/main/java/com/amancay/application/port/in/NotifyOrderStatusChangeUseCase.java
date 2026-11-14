package com.amancay.application.port.in;

import com.amancay.domain.event.OrderStatusChangedEvent;

// Lo usa el consumidor de eventos de pedidos: avisa al comprador si el nuevo estado
// tiene un mail asociado. Devuelve si se mando alguno.
public interface NotifyOrderStatusChangeUseCase {
    boolean notify(OrderStatusChangedEvent event);
}
