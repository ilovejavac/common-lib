package com.dev.lib.jpa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateJsonConfig {
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(ObjectMapper objectMapper) {
        return properties -> properties.put(
                "hibernate.type.json_format_mapper",
                new JacksonJsonFormatMapper(objectMapper)
        );
    }
}