package com.dev.lib.notify.config;

import com.dev.lib.notify.interceptor.ClientIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebNotify 配置类
 */
@Configuration
@EnableConfigurationProperties(SseProperties.class)
@RequiredArgsConstructor
public class WebNotifyConfig implements WebMvcConfigurer {

    private final ClientIdInterceptor clientIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientIdInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/sse/**",  // SSE 端点不需要拦截
                        "/healthz",
                        "/actuator/**",
                        "/error"
                );
    }
}
