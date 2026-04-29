package com.dev.lib.config;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.config.properties.AppSnowFlakeProperties;
import io.github.linpeilie.annotations.ComponentModelConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Map;

import org.slf4j.MDC;

@ComponentModelConfig
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@EnableCaching
@Configuration
@EnableConfigurationProperties({AppSnowFlakeProperties.class, AppSecurityProperties.class})
public class CommonAutoConfig {

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {

        ThreadPoolTaskScheduler pool = new ThreadPoolTaskScheduler();

        pool.setPoolSize(20);
        pool.setThreadNamePrefix("schedule-");
        pool.initialize();

        return pool;
    }

}
