package com.dev.lib.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageObjectBuilderTest {

    @Test
    void bucketShouldRejectBlankName() {

        assertThatThrownBy(() -> Storage.bucket(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bucketName must not be empty");
    }

    @Test
    void batchShouldRejectBlankName() {

        assertThatThrownBy(() -> Storage.batch(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bucketName must not be empty");
    }

    @Test
    void objectShouldRejectBlankKey() {

        assertThatThrownBy(() -> Storage.bucket("docs").object(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("objectKey must not be empty");
    }

    @Test
    void objectShouldTrimLeadingSlashFromKey() {

        String path = Storage.bucket("docs")
                .object("/reports/summary.pdf")
                .path();

        assertThat(path).isEqualTo("docs/reports/summary.pdf");
    }

    @Test
    void objectShouldTrimDuplicatedBucketPrefixFromKey() {

        String path = Storage.bucket("docs")
                .object("docs/reports/summary.pdf")
                .path();

        assertThat(path).isEqualTo("docs/reports/summary.pdf");
    }
}
