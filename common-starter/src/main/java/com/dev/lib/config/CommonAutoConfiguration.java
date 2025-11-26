package com.dev.lib.config;

import com.dev.lib.config.properties.AppCloudProperties;
import com.dev.lib.config.properties.AppDubboProperties;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.config.properties.AppSnowFlakeProperties;
import com.dev.lib.config.properties.AppSqlMonitorProperties;
import com.dev.lib.storage.config.AppStorageProperties;
import io.github.linpeilie.annotations.ComponentModelConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ComponentScan("com.dev.lib")
@ComponentModelConfig
@EnableAsync
@EnableScheduling
public class CommonAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.cloud")
    public AppCloudProperties appCloudProperties() {
        return new AppCloudProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.dubbo")
    public AppDubboProperties appDubboProperties() {
        return new AppDubboProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.security")
    public AppSecurityProperties appSecurityProperties() {
        return new AppSecurityProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.storage")
    public AppStorageProperties appStorageProperties() {
        return new AppStorageProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.snow-flake")
    public AppSnowFlakeProperties appSnowFlakeProperties() {
        return new AppSnowFlakeProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.sql")
    public AppSqlMonitorProperties appSqlMonitorProperties() {
        return new AppSqlMonitorProperties();
    }
}