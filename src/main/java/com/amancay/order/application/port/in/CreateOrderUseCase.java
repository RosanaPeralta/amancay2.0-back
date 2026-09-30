package com.amancay.order.application.port.in;

import com.amancay.order.domain.model.Order;

public interface CreateOrderUseCase {
    Order create(CreateOrderCommand command);
}
