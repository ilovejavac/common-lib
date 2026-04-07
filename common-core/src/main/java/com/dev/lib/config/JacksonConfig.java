package com.dev.lib.config;

import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonFactoryBuilderCustomizer commonJsonFactoryBuilderCustomizer() {

        return JacksonSupport::configure;
    }

    @Bean
    public JsonMapperBuilderCustomizer commonJsonMapperBuilderCustomizer() {

        return JacksonSupport::configure;
    }
}
