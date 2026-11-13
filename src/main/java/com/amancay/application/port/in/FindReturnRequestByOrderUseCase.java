
package com.amancay.application.port.in;

import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.ReturnRequest;

public interface FindReturnRequestByOrderUseCase {

    Optional<ReturnRequest> findByOrderId(UUID requesterId, UUID orderId);
}