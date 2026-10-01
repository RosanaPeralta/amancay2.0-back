package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void clampsPageAndSizeLikeTheControllersDid() {
        assertThat(new PageQuery(-3, 0)).isEqualTo(new PageQuery(0, 1));
        assertThat(new PageQuery(2, 500)).isEqualTo(new PageQuery(2, PageQuery.MAX_SIZE));
    }
}
