package com.dev.lib.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
public class StorageAutoConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.storage")
    public AppStorageProperties appStorageProperties() {

        return new AppStorageProperties();
    }

}
