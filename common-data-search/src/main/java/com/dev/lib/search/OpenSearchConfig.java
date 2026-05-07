package com.dev.lib.search;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@ConditionalOnProperty(prefix = "app.opensearch", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenSearchConfig.OpenSearchProperties.class)
public class OpenSearchConfig {

    @Data
    @ConfigurationProperties(prefix = "app.opensearch")
    public static class OpenSearchProperties {

        private boolean enabled = false;

        private String index = "public";

    }

}
