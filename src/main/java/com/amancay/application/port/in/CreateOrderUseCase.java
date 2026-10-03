package com.amancay.application.port.in;

import com.amancay.domain.model.Order;

public interface CreateOrderUseCase {
    Order create(CreateOrderCommand command);
}
