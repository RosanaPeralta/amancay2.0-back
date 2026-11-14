package com.amancay.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Consulta de pagos en un proveedor externo (Mercado Pago). El cobro nunca confia en lo
// que llega en una notificacion: siempre vuelve a leer el pago desde aca.
public interface PaymentProviderPort {

    Optional<ProviderPayment> findPayment(String providerPaymentId);

    // externalReference es el id de la orden, que se manda al crear el checkout.
    List<ProviderPayment> findPaymentsByExternalReference(String externalReference);

    record ProviderPayment(String id, String status, BigDecimal amount, String externalReference) {

        public boolean isApproved() {
            return "approved".equals(status);
        }
    }
}
