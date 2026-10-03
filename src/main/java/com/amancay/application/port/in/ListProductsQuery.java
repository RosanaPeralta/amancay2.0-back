package com.amancay.application.port.in;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.ProductFilter;
import com.amancay.domain.model.ProductSort;
import com.amancay.domain.model.ProductSummary;

public interface ListProductsQuery {
    PageResult<ProductSummary> list(ProductFilter filter, ProductSort sort, PageQuery page);
}
