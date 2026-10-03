package com.amancay.application.port.in;

import java.util.UUID;

public interface DeleteProductUseCase {
    void delete(UUID requesterId, UUID id);
}
