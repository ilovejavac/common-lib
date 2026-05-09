package com.dev.lib.web.interceptor;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@FunctionalInterface
public interface CommonMvcInterceptorRegistration {

    void addInterceptors(InterceptorRegistry registry);
}
