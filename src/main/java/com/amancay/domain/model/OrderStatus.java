package com.amancay.domain.model;

import java.util.EnumSet;
import java.util.Set;

// Patron State: cada estado sabe a cuales puede pasar y quien dispara ese paso
// (el pago, la logistica externa o una devolucion aprobada).
public enum OrderStatus {
    CREADO(OrderTransitionTrigger.PAYMENT) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(EN_PREPARACION);
        }
    },
    EN_PREPARACION(OrderTransitionTrigger.LOGISTICS) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(LISTO_PARA_DESPACHAR);
        }
    },
    LISTO_PARA_DESPACHAR(OrderTransitionTrigger.LOGISTICS) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(DESPACHADO);
        }
    },
    DESPACHADO(OrderTransitionTrigger.LOGISTICS) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(ENTREGADO);
        }
    },
    ENTREGADO(OrderTransitionTrigger.RETURN_REQUEST) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(ESPERANDO_RETIRO);
        }
    },
    // Devolucion aprobada: la logistica tiene que pasar a buscar el pedido.
    ESPERANDO_RETIRO(OrderTransitionTrigger.LOGISTICS) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(RETIRADO);
        }
    },
    RETIRADO(OrderTransitionTrigger.LOGISTICS) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.of(DEVUELTO);
        }
    },
    DEVUELTO(null) {
        @Override
        public Set<OrderStatus> nextStates() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    };

    private final OrderTransitionTrigger advancedBy;

    OrderStatus(OrderTransitionTrigger advancedBy) {
        this.advancedBy = advancedBy;
    }

    public abstract Set<OrderStatus> nextStates();

    public boolean canTransitionTo(OrderStatus target) {
        return nextStates().contains(target);
    }

    // DEVUELTO es final: nadie lo hace avanzar.
    public boolean isAdvancedBy(OrderTransitionTrigger trigger) {
        return advancedBy != null && advancedBy == trigger;
    }
}
