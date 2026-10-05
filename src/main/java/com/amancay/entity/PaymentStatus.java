package com.amancay.entity;

import java.util.EnumSet;
import java.util.Set;

// Patron State implementado como enum, igual que OrderStatus: cada constante sabe a que
// estados puede pasar. APROBADO y RECHAZADO son terminales para esa fila de Payment; un
// reintento no los transiciona, crea un Payment nuevo (ver PaymentService.retryPayment).
public enum PaymentStatus {
    PENDIENTE {
        @Override
        public Set<PaymentStatus> nextStates() {
            return EnumSet.of(APROBADO, RECHAZADO);
        }
    },
    APROBADO {
        @Override
        public Set<PaymentStatus> nextStates() {
            return EnumSet.noneOf(PaymentStatus.class);
        }
    },
    RECHAZADO {
        @Override
        public Set<PaymentStatus> nextStates() {
            return EnumSet.noneOf(PaymentStatus.class);
        }
    };

    public abstract Set<PaymentStatus> nextStates();

    public boolean canTransitionTo(PaymentStatus target) {
        return nextStates().contains(target);
    }
}
