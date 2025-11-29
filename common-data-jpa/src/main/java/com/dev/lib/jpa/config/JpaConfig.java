package com.dev.lib.jpa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    @Bean
    @ConfigurationProperties(prefix = "app.sql")
    public AppSqlMonitorProperties appSqlMonitorProperties() {
        return new AppSqlMonitorProperties();
    }
}