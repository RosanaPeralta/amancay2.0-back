
package com.amancay.domain.model;

public enum ReturnRequestStatus {
    PENDIENTE,
    APROBADA,
    RECHAZADA;

    public boolean isPending() {
        return this == PENDIENTE;
    }

    public boolean isResolved() {
        return this == APROBADA || this == RECHAZADA;
    }
}