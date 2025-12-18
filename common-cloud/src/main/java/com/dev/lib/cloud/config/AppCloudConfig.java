package com.dev.lib.cloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppCloudConfig {

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

}
