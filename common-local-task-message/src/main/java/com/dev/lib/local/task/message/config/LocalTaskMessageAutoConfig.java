package com.dev.lib.local.task.message.config;

import com.dev.lib.local.task.message.domain.adapter.DefaultRabbitMQ;
import com.dev.lib.local.task.message.domain.adapter.IRabbitPublish;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalTaskMessageAutoConfig {

    @Bean
    @ConditionalOnMissingBean(IRabbitPublish.class)
    public IRabbitPublish rabbitPublish() {

        return new DefaultRabbitMQ();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.local-task-message")
    public LocalTaskConfigProperties localTaskConfigProperties() {

        return new LocalTaskConfigProperties();
    }

}
