package com.amancay.application.port.in;

import java.util.UUID;

// Lo usa el cobro al aprobar un pago: no exige ADMIN y ningun controller lo expone.
public interface MarkOrderAsPaidUseCase {
    void markAsPaid(UUID orderId);
}
