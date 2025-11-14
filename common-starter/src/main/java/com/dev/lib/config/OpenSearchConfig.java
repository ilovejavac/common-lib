package com.dev.lib.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchConfig {

    @Bean
    public RestHighLevelClient openSearchClient(OpenSearchProperties properties) {
        String[] hosts = properties.getHosts().split(",");
        HttpHost[] httpHosts = new HttpHost[hosts.length];

        for (int i = 0; i < hosts.length; i++) {
            String[] parts = hosts[i].split(":");
            httpHosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]), "http");
        }

        return new RestHighLevelClient(RestClient.builder(httpHosts));
    }

    @Bean
    @ConfigurationProperties(prefix = "app.opensearch")
    public OpenSearchProperties openSearchProperties() {
        return new OpenSearchProperties();
    }

    @Data
    public static class OpenSearchProperties {
        private boolean enabled = false;
        private String hosts = "localhost:9200";
        private String indexPrefix = "app-logs";
    }
}