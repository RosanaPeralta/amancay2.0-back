
package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.ReturnRequest;

public interface ApproveReturnRequestUseCase {
    ReturnRequest approve(UUID adminId, UUID requestId);
}
