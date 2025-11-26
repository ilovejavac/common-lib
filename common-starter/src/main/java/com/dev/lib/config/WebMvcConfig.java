package com.dev.lib.config;

import com.dev.lib.web.interceptor.AuthInterceptor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final ObjectMapper objectMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册权限校验拦截器
        registry.addInterceptor(authInterceptor)
                .order(20)
                .addPathPatterns("/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper webMapper = objectMapper.copy();
        webMapper.addMixIn(Object.class, BaseEntityMixIn.class);
        webMapper.registerModule(new SimpleModule()
                .addSerializer(Long.class, new LongToStringSerializer())
                .addSerializer(Long.TYPE, new LongToStringSerializer())
                .addDeserializer(Long.class, new StringToLongDeserializer())
                .addDeserializer(Long.TYPE, new StringToLongDeserializer())
        );

        converters.add(0, new MappingJackson2HttpMessageConverter(webMapper));
    }

    interface BaseEntityMixIn {
        @JsonIgnore
        Long getId();

        @JsonIgnore
        String getReversion();

        @JsonIgnore
        Boolean getDeleted();
    }

    static class LongToStringSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    static class StringToLongDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}