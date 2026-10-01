package com.amancay.application.service.fake;

import java.util.List;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;

final class Pages {

    private Pages() {
    }

    static <T> PageResult<T> of(List<T> all, PageQuery query) {
        int from = Math.min(query.page() * query.size(), all.size());
        int to = Math.min(from + query.size(), all.size());
        int totalPages = (int) Math.ceil((double) all.size() / query.size());
        return new PageResult<>(all.subList(from, to), query.page(), query.size(), all.size(), totalPages);
    }
}
