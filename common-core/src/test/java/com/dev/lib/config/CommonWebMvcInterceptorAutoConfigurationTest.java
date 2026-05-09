package com.dev.lib.config;

import com.dev.lib.web.interceptor.CommonMvcInterceptorRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonWebMvcInterceptorAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(CommonWebMvcInterceptorAutoConfiguration.class, TestRegistrationConfig.class);

    @Test
    void webMvcConfigurerShouldApplyRegisteredInterceptorsInOrder() {

        contextRunner.run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            InterceptorRegistry registry = new InterceptorRegistry();

            configurer.addInterceptors(registry);

            List<MappedInterceptor> mappedInterceptors = mappedInterceptors(registry);
            assertThat(mappedInterceptors).hasSize(2);
            assertThat(mappedInterceptors.get(0).getInterceptor()).isSameAs(TestRegistrationConfig.FIRST);
            assertThat(mappedInterceptors.get(0).getIncludePathPatterns()).containsExactly("/first/**");
            assertThat(mappedInterceptors.get(1).getInterceptor()).isSameAs(TestRegistrationConfig.SECOND);
            assertThat(mappedInterceptors.get(1).getIncludePathPatterns()).containsExactly("/second/**");
        });
    }

    private List<MappedInterceptor> mappedInterceptors(InterceptorRegistry registry) throws Exception {

        Method getInterceptors = InterceptorRegistry.class.getDeclaredMethod("getInterceptors");
        getInterceptors.setAccessible(true);
        return ((List<?>) getInterceptors.invoke(registry)).stream()
                .filter(MappedInterceptor.class::isInstance)
                .map(MappedInterceptor.class::cast)
                .toList();
    }

    @Configuration
    static class TestRegistrationConfig {

        private static final HandlerInterceptor FIRST = new HandlerInterceptor() {
        };

        private static final HandlerInterceptor SECOND = new HandlerInterceptor() {
        };

        @Bean
        CommonMvcInterceptorRegistration secondRegistration() {

            return registry -> registry.addInterceptor(SECOND)
                    .order(20)
                    .addPathPatterns("/second/**");
        }

        @Bean
        CommonMvcInterceptorRegistration firstRegistration() {

            return registry -> registry.addInterceptor(FIRST)
                    .order(10)
                    .addPathPatterns("/first/**");
        }
    }
}
