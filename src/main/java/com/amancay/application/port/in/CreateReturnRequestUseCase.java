
package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.ReturnReason;
import com.amancay.domain.model.ReturnRequest;

public interface CreateReturnRequestUseCase {

    ReturnRequest create(
            UUID requesterId,
            UUID orderId,
            ReturnReason reason,
            String comment);
}