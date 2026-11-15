package com.amancay.application.port.in;

// Lo usa el consumidor de la cola de logistica: no exige ADMIN y ningun controller lo expone.
public interface ApplyOrderStatusCommandUseCase {

    Result apply(AdvanceOrderStatusCommand command);

    enum Result {
        APPLIED,
        // La orden ya estaba en ese estado: el mensaje se entrego dos veces.
        ALREADY_APPLIED
    }
}
