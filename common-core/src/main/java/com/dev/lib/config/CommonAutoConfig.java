package com.dev.lib.config;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.config.properties.AppSnowFlakeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties({AppSnowFlakeProperties.class, AppSecurityProperties.class})
public class CommonAutoConfig {

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {

        ThreadPoolTaskScheduler pool = new ThreadPoolTaskScheduler();

        pool.setPoolSize(20);
        pool.setThreadNamePrefix("lib-scheduler-");
        pool.initialize();

        return pool;
    }

}
