package com.amancay.application.port.in;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;

public interface ListProductsQuery {
    // Productos completos (variantes, imagenes, descuento) para dibujar la tarjeta sin otra llamada.
    PageResult<Product> list(ProductFilter filter, ProductSort sort, PageQuery page);
}
