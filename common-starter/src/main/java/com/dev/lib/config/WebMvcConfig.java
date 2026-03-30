package com.dev.lib.config;

import com.fasterxml.jackson.databind.Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcConfig {

    @Bean
    public Module populateFieldJacksonModule() {

        return new PopulateFieldBeanSerializerModifier().asModule();
    }
}
