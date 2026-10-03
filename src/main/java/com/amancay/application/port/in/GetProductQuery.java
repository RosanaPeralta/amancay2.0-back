package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Product;

public interface GetProductQuery {
    Product getById(UUID id);
}
