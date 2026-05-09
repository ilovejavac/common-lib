package com.dev.lib.config;

import com.dev.lib.web.interceptor.CommonMvcInterceptorRegistration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebMvcInterceptorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "commonWebMvcInterceptorConfigurer")
    public WebMvcConfigurer commonWebMvcInterceptorConfigurer(
            ObjectProvider<CommonMvcInterceptorRegistration> registrations
    ) {

        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {

                registrations.orderedStream()
                        .forEach(registration -> registration.addInterceptors(registry));
            }
        };
    }
}
