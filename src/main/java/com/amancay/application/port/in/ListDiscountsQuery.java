package com.amancay.application.port.in;

import java.util.List;

import com.amancay.domain.model.Discount;

public interface ListDiscountsQuery {
    List<Discount> listAll();

    Discount getById(Long id);

    // Con descripcion vacia devuelve todos.
    List<Discount> searchByDescription(String description);
}
