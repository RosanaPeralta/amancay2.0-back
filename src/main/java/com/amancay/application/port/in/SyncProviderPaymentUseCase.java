package com.amancay.application.port.in;

// Aviso del proveedor (webhook de Mercado Pago). No hay usuario logueado: el pago se lee
// del proveedor y se aplica solo si esta aprobado y coincide con una orden pendiente.
public interface SyncProviderPaymentUseCase {
    void syncFromProvider(String providerPaymentId);
}
