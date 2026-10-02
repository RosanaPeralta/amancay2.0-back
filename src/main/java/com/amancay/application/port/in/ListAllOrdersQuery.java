package com.amancay.application.port.in;

import java.util.List;

import com.amancay.domain.model.Order;

public interface ListAllOrdersQuery {

    // Ordenes de TODOS los usuarios, mas nuevas primero: el panel de pedidos del admin.
    List<AdminOrder> listAll();

    record AdminOrder(Order order, String buyerEmail) {
    }
}
