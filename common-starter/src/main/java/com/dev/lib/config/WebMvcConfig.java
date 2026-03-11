package com.dev.lib.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer webJacksonCustomizer() {

        PopulateFieldBeanSerializerModifier modifier = new PopulateFieldBeanSerializerModifier();
        return builder -> builder.postConfigurer(mapper -> mapper.registerModule(modifier.asModule()));
    }
}
