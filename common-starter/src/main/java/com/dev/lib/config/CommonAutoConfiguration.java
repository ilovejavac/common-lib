package com.dev.lib.config;

import com.dev.lib.config.properties.LogstashProperties;
import io.github.linpeilie.annotations.ComponentModelConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ComponentScan("com.dev.lib")
@ComponentModelConfig
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
public class CommonAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.logstash")
    public LogstashProperties logstashProperties() {
        return new LogstashProperties();
    }
}