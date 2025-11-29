package com.dev.lib.config;

import com.dev.lib.config.properties.AppSnowFlakeProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonAutoConfig {
    @Bean
    @ConfigurationProperties(prefix = "app.snow-flake")
    public AppSnowFlakeProperties appSnowFlakeProperties() {
        return new AppSnowFlakeProperties();
    }


}
