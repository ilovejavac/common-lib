package com.dev.lib.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class CommonJacksonAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public JsonMapperBuilderCustomizer commonJsonMapperBuilderCustomizer() {

        return JacksonSupport::customize;
    }
}
