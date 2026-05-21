package com.dev.lib.config;

import com.dev.lib.config.properties.LogstashProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableFeignClients
@ComponentScan("com.dev.lib")
public class CommonAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.logstash")
    public LogstashProperties logstashProperties() {

        return new LogstashProperties();
    }

}
