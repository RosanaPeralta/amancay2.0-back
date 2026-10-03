package com.amancay.application.port.in;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItemProduct;

public interface DescribeOrderItemsQuery {
    // Producto de cada linea de los pedidos dados, indexado por variantId.
    Map<UUID, OrderItemProduct> describe(Collection<Order> orders);
}
