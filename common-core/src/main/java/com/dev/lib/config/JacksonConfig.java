package com.dev.lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {

        ObjectMapper objectMapper = JsonMapper.builder().build();
        JacksonSupport.configure(objectMapper);
        return objectMapper;
    }

    public static ObjectMapper configure(ObjectMapper objectMapper) {

        JacksonSupport.configure(objectMapper);
        return objectMapper;
    }
}
