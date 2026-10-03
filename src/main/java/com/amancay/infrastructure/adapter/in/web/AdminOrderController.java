package com.amancay.infrastructure.adapter.in.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.DescribeOrderItemsQuery;
import com.amancay.application.port.in.ListAllOrdersQuery;
import com.amancay.application.port.in.ListAllOrdersQuery.AdminOrder;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.infrastructure.adapter.in.web.dto.AdminOrderResponse;
import com.amancay.infrastructure.security.LoggedUser;

/**
 * Listado de pedidos de todos los usuarios para el panel de admin. El cambio de estado
 * sigue siendo PATCH /api/orders/{id}/status.
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final ListAllOrdersQuery listAllOrdersQuery;
    private final DescribeOrderItemsQuery describeOrderItemsQuery;

    public AdminOrderController(ListAllOrdersQuery listAllOrdersQuery, DescribeOrderItemsQuery describeOrderItemsQuery) {
        this.listAllOrdersQuery = listAllOrdersQuery;
        this.describeOrderItemsQuery = describeOrderItemsQuery;
    }

    @GetMapping
    public ResponseEntity<List<AdminOrderResponse>> list(@AuthenticationPrincipal LoggedUser loggedUser) {
        List<AdminOrder> orders = listAllOrdersQuery.listAll(loggedUser.id());
        Map<UUID, OrderItemProduct> products = describeOrderItemsQuery.describe(
                orders.stream().map(AdminOrder::order).toList());
        return ResponseEntity.ok(orders.stream()
                .map(order -> AdminOrderResponse.from(order, products))
                .toList());
    }
}
