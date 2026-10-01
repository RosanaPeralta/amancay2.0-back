package com.amancay.application.port.in;

import com.amancay.domain.model.Product;

public interface CreateProductUseCase {
    Product create(CreateProductCommand command);
}
