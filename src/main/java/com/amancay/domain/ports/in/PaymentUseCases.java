package com.amancay.domain.ports.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.PaymentMethodType;
import com.amancay.domain.model.PaymentRecord;
import com.amancay.domain.model.PaymentState;
import com.amancay.domain.model.PendingPayment;

public interface PaymentUseCases {
    List<PaymentRecord> listByOrder(UUID requesterId, UUID orderId);

    PaymentRecord create(UUID requesterId, UUID orderId, PaymentRequest request);

    PaymentRecord retry(UUID requesterId, UUID paymentId, PaymentRequest request);

    List<PendingPayment> listPendingForAdmin();

    PaymentRecord confirm(UUID paymentId, PaymentState state);

    PaymentRecord attachTransferReference(UUID requesterId, UUID paymentId, String transferReference);

    record PaymentRequest(PaymentMethodType method, CardData card) {
    }

    record CardData(String number, String holderName, String expiry, String cvv) {
    }
}