package com.amancay.domain.model;

import java.util.EnumSet;
import java.util.Set;

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

    // Lo que el admin puede decidir sobre un pago pendiente.
    public boolean isFinal() {
        return this == APROBADO || this == RECHAZADO;
    }
}
