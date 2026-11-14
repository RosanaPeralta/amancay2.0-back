
package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.ReturnRequest;

public interface RejectReturnRequestUseCase {
    ReturnRequest reject(UUID adminId, UUID requestId, String resolutionNote);
}