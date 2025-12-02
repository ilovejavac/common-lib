package com.dev.lib.config;

import com.dev.lib.config.properties.AppSnowFlakeProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CommonAutoConfig {
    @Bean
    @ConfigurationProperties(prefix = "app.snow-flake")
    public AppSnowFlakeProperties appSnowFlakeProperties() {
        return new AppSnowFlakeProperties();
    }


    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler pool = new ThreadPoolTaskScheduler();

        pool.setPoolSize(20);
        pool.setThreadNamePrefix("lib-scheduler-");
        pool.initialize();

        return pool;
    }
}
