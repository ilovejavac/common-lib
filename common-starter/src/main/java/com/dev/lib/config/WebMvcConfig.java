package com.dev.lib.config;

import com.dev.lib.web.interceptor.AuthInterceptor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

        converters.add(0, new MappingJackson2HttpMessageConverter(webMapper));
    }

    interface BaseEntityMixIn {
        @JsonIgnore
        Long getId();

        @JsonIgnore
        String getVersion();

        @JsonIgnore
        Boolean getDeleted();
    }

}