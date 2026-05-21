package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.annotation.Aksk;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskHeaders;

public class AkskHeaderResolver {

    public AkskHeaders resolve(Aksk annotation, AkskProperties properties) {

        AkskProperties source = properties == null ? new AkskProperties() : properties;
        String annotationPrefix = annotation == null ? "" : annotation.headerPrefix();
        String propertyPrefix = source.getHeaderPrefix();

        String accessKeyHeader = resolveHeader(
                annotation == null ? "" : annotation.accessKeyHeader(),
                annotationPrefix,
                source.getAccessKeyHeader(),
                propertyPrefix,
                "Access-Key"
        );
        String timestampHeader = resolveHeader(
                annotation == null ? "" : annotation.timestampHeader(),
                annotationPrefix,
                source.getTimestampHeader(),
                propertyPrefix,
                "Timestamp"
        );
        String signatureHeader = resolveHeader(
                annotation == null ? "" : annotation.signatureHeader(),
                annotationPrefix,
                source.getSignatureHeader(),
                propertyPrefix,
                "Signature"
        );

        return new AkskHeaders(accessKeyHeader, timestampHeader, signatureHeader);
    }

    private String resolveHeader(
            String annotationHeader,
            String annotationPrefix,
            String propertyHeader,
            String propertyPrefix,
            String suffix
    ) {

        if (hasText(annotationHeader)) {
            return annotationHeader;
        }
        if (hasText(annotationPrefix)) {
            return derive(annotationPrefix, suffix);
        }
        if (hasText(propertyHeader)) {
            return propertyHeader;
        }
        return derive(propertyPrefix, suffix);
    }

    private String derive(String prefix, String suffix) {

        String resolvedPrefix = hasText(prefix) ? prefix : "X-Aksk";
        return resolvedPrefix + "-" + suffix;
    }

    private boolean hasText(String value) {

        return value != null && !value.isBlank();
    }
}
