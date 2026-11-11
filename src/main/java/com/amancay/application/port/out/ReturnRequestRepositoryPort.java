
package com.amancay.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.ReturnRequest;
import com.amancay.domain.model.ReturnRequestStatus;

public interface ReturnRequestRepositoryPort {

    Optional<ReturnRequest> findById(UUID id);

    Optional<ReturnRequest> findByOrderId(UUID orderId);

    List<ReturnRequest> findByStatus(ReturnRequestStatus status);

    ReturnRequest save(ReturnRequest request);
}