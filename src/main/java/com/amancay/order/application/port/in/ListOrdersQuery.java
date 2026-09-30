package com.amancay.order.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.order.domain.model.Order;

public interface ListOrdersQuery {
    List<Order> list(UUID requesterId, UUID requestedUserId);
}
