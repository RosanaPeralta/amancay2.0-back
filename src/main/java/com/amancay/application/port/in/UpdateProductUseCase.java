package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Product;

public interface UpdateProductUseCase {
    Product update(UUID id, UpdateProductCommand command);
}
