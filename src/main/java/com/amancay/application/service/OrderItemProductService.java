package com.amancay.application.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.DescribeOrderItemsQuery;
import com.amancay.application.port.out.LoadOrderItemProductsPort;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderItemProduct;

@Service
public class OrderItemProductService implements DescribeOrderItemsQuery {

    private final LoadOrderItemProductsPort loadOrderItemProducts;

    public OrderItemProductService(LoadOrderItemProductsPort loadOrderItemProducts) {
        this.loadOrderItemProducts = loadOrderItemProducts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, OrderItemProduct> describe(Collection<Order> orders) {
        Set<UUID> variantIds = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::productVariantId)
                .collect(Collectors.toSet());
        return variantIds.isEmpty() ? Map.of() : loadOrderItemProducts.findByVariantIds(variantIds);
    }
}
