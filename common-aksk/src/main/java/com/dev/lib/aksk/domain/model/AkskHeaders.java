package com.dev.lib.aksk.domain.model;

public record AkskHeaders(
        String accessKeyHeader,
        String timestampHeader,
        String signatureHeader
) {
}
