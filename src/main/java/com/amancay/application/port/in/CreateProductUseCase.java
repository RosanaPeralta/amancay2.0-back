package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Product;

public interface CreateProductUseCase {
    Product create(UUID requesterId, CreateProductCommand command);
}
