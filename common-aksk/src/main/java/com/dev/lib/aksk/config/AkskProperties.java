package com.dev.lib.aksk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
public class AkskProperties {

    private String headerPrefix = "X-Aksk";

    private String accessKeyHeader;

    private String timestampHeader;

    private String signatureHeader;

    private Duration timestampTolerance = Duration.ofMinutes(5);

    private int maxCachedBodyBytes = 1024 * 1024;

}
