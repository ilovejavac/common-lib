package com.dev.lib.web.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRequestTest {

    @Test
    void toPageableShouldAllowFrontendPageSize1024() {

        QueryRequest<Object> request = new QueryRequest<>();
        request.setQuery(new Object());
        request.setSize(1024);

        assertThat(request.toPageable(Set.of()).getPageSize()).isEqualTo(1024);
    }

    @Test
    void toPageableShouldPreservePageOutsideVisibleRangeForEmptyResult() {

        QueryRequest<Object> request = new QueryRequest<>();
        request.setQuery(new Object());
        request.setPage(65);
        request.setSize(1024);

        Pageable pageable = request.toPageable(Set.of());

        assertThat(pageable.getPageNumber()).isEqualTo(64);
        assertThat(pageable.getPageSize()).isEqualTo(1024);
        assertThat(pageable.getOffset()).isEqualTo(65536);
    }
}
