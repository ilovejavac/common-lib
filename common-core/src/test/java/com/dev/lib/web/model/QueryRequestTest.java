package com.dev.lib.web.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRequestTest {

    @Test
    void toPageableShouldAllowFrontendPageSize256() {

        QueryRequest<Object> request = new QueryRequest<>();
        request.setQuery(new Object());
        request.setSize(256);

        assertThat(request.toPageable(Set.of()).getPageSize()).isEqualTo(256);
    }
}
