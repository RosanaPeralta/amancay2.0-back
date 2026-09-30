package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.PaymentRecord;
import com.amancay.domain.model.PendingPayment;

public interface PaymentCatalogPort {
    List<PaymentRecord> findByOrderId(UUID orderId);

    Optional<PaymentRecord> findById(UUID paymentId);

    boolean hasApprovedPayment(UUID orderId);

    PaymentRecord save(PaymentRecord payment);

    List<PendingPayment> findPendingForAdmin();
}