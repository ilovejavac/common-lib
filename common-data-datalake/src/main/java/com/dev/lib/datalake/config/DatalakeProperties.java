package com.dev.lib.datalake.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.datalake")
public class DatalakeProperties {

    private DorisProperties doris = new DorisProperties();

    private HttpEndpointProperties clickhouse = new HttpEndpointProperties();

    private HttpEndpointProperties hive = new HttpEndpointProperties();

    @Data
    public static class DorisProperties extends DorisStreamLoadProperties {

        private boolean enabled = true;
    }

    @Data
    public static class DorisStreamLoadProperties {

        private List<String> streamLoadUrls = new ArrayList<>();

        private String username;

        private String password;

        private String database;

        private String table;

        private String labelPrefix;

        private Duration connectTimeout = Duration.ofSeconds(5);

        private Duration readTimeout = Duration.ofSeconds(60);

        private int maxRetries = 2;

        private Map<String, String> headers = new LinkedHashMap<>();
    }

    @Data
    public static class HttpEndpointProperties {

        private boolean enabled = true;

        private List<String> urls = new ArrayList<>();

        private String username;

        private String password;

        private String database;

        private Duration connectTimeout = Duration.ofSeconds(5);

        private Duration readTimeout = Duration.ofSeconds(60);

        private int maxRetries = 2;

        private Map<String, String> headers = new LinkedHashMap<>();
    }
}
