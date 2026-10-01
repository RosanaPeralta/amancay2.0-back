package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;

public interface ListFavoritesQuery {
    // Pagina de favoritos con los datos del producto resueltos en una sola consulta.
    PageResult<FavoriteProduct> list(UUID userId, PageQuery page);

    // Solo los ids, para que el front marque el corazon en el catalogo sin paginar.
    List<UUID> listProductIds(UUID userId);
}
