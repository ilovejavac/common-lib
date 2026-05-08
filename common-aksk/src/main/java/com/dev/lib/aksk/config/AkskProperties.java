package com.dev.lib.aksk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.aksk")
public class AkskProperties {

    private String headerPrefix = "X-Aksk";

    private String accessKeyHeader;

    private String timestampHeader;

    private String signatureHeader;

    private Duration timestampTolerance = Duration.ofMinutes(5);

    private int maxCachedBodyBytes = 1024 * 1024;

    public String getHeaderPrefix() {

        return headerPrefix;
    }

    public void setHeaderPrefix(String headerPrefix) {

        this.headerPrefix = headerPrefix;
    }

    public String getAccessKeyHeader() {

        return accessKeyHeader;
    }

    public void setAccessKeyHeader(String accessKeyHeader) {

        this.accessKeyHeader = accessKeyHeader;
    }

    public String getTimestampHeader() {

        return timestampHeader;
    }

    public void setTimestampHeader(String timestampHeader) {

        this.timestampHeader = timestampHeader;
    }

    public String getSignatureHeader() {

        return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {

        this.signatureHeader = signatureHeader;
    }

    public Duration getTimestampTolerance() {

        return timestampTolerance;
    }

    public void setTimestampTolerance(Duration timestampTolerance) {

        this.timestampTolerance = timestampTolerance;
    }

    public int getMaxCachedBodyBytes() {

        return maxCachedBodyBytes;
    }

    public void setMaxCachedBodyBytes(int maxCachedBodyBytes) {

        this.maxCachedBodyBytes = maxCachedBodyBytes;
    }
}
