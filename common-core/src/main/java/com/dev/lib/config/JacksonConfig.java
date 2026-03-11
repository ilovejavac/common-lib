package com.dev.lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {

        return builder -> builder.postConfigurer(JacksonSupport::configure);
    }

    public static ObjectMapper configure(ObjectMapper objectMapper) {

        JacksonSupport.configure(objectMapper);
        return objectMapper;
    }
}
