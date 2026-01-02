package com.dev.lib.storage.config;

import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = "com.dev.lib.storage")
@EnableJpaRepositories(
        repositoryBaseClass = BaseRepositoryImpl.class
)
@ComponentScan
@Configuration
public class StorageAutoConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.storage")
    public AppStorageProperties appStorageProperties() {

        return new AppStorageProperties();
    }

}
