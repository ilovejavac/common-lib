package com.dev.lib.aksk.web;

import com.dev.lib.web.interceptor.CommonMvcInterceptorRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Component
@RequiredArgsConstructor
public class AkskMvcInterceptorRegistration implements CommonMvcInterceptorRegistration {

    private final AkskAuthenticationInterceptor akskAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(akskAuthenticationInterceptor)
                .order(15)
                .addPathPatterns("/**");
    }
}
