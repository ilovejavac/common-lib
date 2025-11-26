package com.dev.lib.config;

import lombok.Data;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchConfig {

    @Bean
    public OpenSearchClient openSearchClient(OpenSearchProperties properties) {
        HttpHost[] httpHosts = parseHosts(properties.getHosts());

        var transport = ApacheHttpClient5TransportBuilder
                .builder(httpHosts)
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setConnectionManager(
                                PoolingAsyncClientConnectionManagerBuilder.create().build()
                        )
                )
                .build();

        return new OpenSearchClient(transport);
    }

    private HttpHost[] parseHosts(String hosts) {
        String[] hostArray = hosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hostArray.length];

        for (int i = 0; i < hostArray.length; i++) {
            String[] parts = hostArray[i].trim().split(":");
            httpHosts[i] = new HttpHost("http", parts[0], Integer.parseInt(parts[1]));
        }
        return httpHosts;
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