package com.amancay.domain.model;

public enum OrderState {
    CREADO,
    EN_PREPARACION,
    DESPACHADO,
    ENTREGADO,
    DEVUELTO;

    public boolean canTransitionTo(OrderState target) {
        return switch (this) {
            case CREADO -> target == EN_PREPARACION;
            case EN_PREPARACION -> target == DESPACHADO;
            case DESPACHADO -> target == ENTREGADO;
            case ENTREGADO -> target == DEVUELTO;
            case DEVUELTO -> false;
        };
    }
}