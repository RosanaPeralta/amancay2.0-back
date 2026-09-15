package com.amancay.entity;

import java.util.EnumSet;
import java.util.Set;

// Patron State implementado como enum: cada constante sabe a que estados puede
// pasar. Evita que un OrderService (o cualquier otro caller) fuerce una
// transicion invalida como ENTREGADO -> CREADO.
public enum OrderStatus {
    CREADO {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(EN_PREPARACION);
        }
    },
    EN_PREPARACION {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(DESPACHADO);
        }
    },
    DESPACHADO {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(ENTREGADO);
        }
    },
    ENTREGADO {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(DEVUELTO);
        }
    },
    DEVUELTO {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    };

    public abstract Set<OrderStatus> nextStates();

    public boolean canTransitionTo(OrderStatus target) {
        return nextStates().contains(target);
    }
}
