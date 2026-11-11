package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.ReturnRequest;

public interface ListReturnRequestsUseCase {

    List<ReturnRequest> listPending(UUID adminId);
}