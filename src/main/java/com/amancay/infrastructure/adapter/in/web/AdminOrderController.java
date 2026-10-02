package com.amancay.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.ListAllOrdersQuery;
import com.amancay.infrastructure.adapter.in.web.dto.AdminOrderResponse;

/**
 * Listado de pedidos de todos los usuarios para el panel de admin. El cambio de estado
 * sigue siendo PATCH /api/orders/{id}/status.
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final ListAllOrdersQuery listAllOrdersQuery;

    public AdminOrderController(ListAllOrdersQuery listAllOrdersQuery) {
        this.listAllOrdersQuery = listAllOrdersQuery;
    }

    @GetMapping
    public ResponseEntity<List<AdminOrderResponse>> list() {
        return ResponseEntity.ok(listAllOrdersQuery.listAll().stream()
                .map(AdminOrderResponse::from)
                .toList());
    }
}
