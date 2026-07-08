package com.dev.lib.jpa.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BaseRepositoryImplBatchSizeTest {

    @Test
    void defaultJdbcBatchSizeShouldBe128() {

        assertThat(ReflectionTestUtils.getField(BaseRepositoryImpl.class, "DEFAULT_JDBC_BATCH_SIZE"))
                .isEqualTo(128);
    }
}
