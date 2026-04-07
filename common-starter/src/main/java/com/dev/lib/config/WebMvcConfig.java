package com.dev.lib.config;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.json.JsonFactoryBuilder;

import java.util.List;

@Configuration
public class WebMvcConfig {

    @Bean
    public JacksonModule populateFieldJacksonModule() {

        return new PopulateFieldBeanSerializerModifier().asModule();
    }

    @Bean
    public JacksonJsonHttpMessageConverter commonJacksonJsonHttpMessageConverter(List<JacksonModule> modules) {

        JsonFactoryBuilder factoryBuilder = JsonFactory.builder();
        JacksonSupport.configure(factoryBuilder);

        JsonMapper.Builder builder = JsonMapper.builder(factoryBuilder.build());
        JacksonSupport.configure(builder);
        builder.addModules(modules);
        return new JacksonJsonHttpMessageConverter(builder.build());
    }

    @Bean
    public ServerHttpMessageConvertersCustomizer commonServerHttpMessageConvertersCustomizer(
            JacksonJsonHttpMessageConverter commonJacksonJsonHttpMessageConverter
    ) {

        return builder -> builder.withJsonConverter(commonJacksonJsonHttpMessageConverter);
    }

    @Bean
    public ClientHttpMessageConvertersCustomizer commonClientHttpMessageConvertersCustomizer(
            JacksonJsonHttpMessageConverter commonJacksonJsonHttpMessageConverter
    ) {

        return builder -> builder.withJsonConverter(commonJacksonJsonHttpMessageConverter);
    }
}
