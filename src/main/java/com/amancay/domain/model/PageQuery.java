package com.amancay.domain.model;

// Pagina pedida por el cliente. Normaliza los valores igual que hacian los controllers:
// pagina minima 0 y tamano entre 1 y 100.
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
