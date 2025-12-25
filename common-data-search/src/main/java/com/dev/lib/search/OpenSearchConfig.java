package com.dev.lib.search;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.opensearch")
    public OpenSearchProperties openSearchProperties() {

        return new OpenSearchProperties();
    }

    @Data
    public static class OpenSearchProperties {

        private boolean enabled = false;

        private String index = "public";

    }

}