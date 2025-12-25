package com.dev.lib.security.config;

import com.dev.lib.security.interceptor.AuthInterceptor;
import com.dev.lib.security.interceptor.InternalInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ComponentScan("com.dev.lib.security")
public class WebSecurityConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final InternalInterceptor internalFilter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(internalFilter)
                .order(10)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**");

        registry.addInterceptor(authInterceptor)
                .order(20)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**");
    }

}
