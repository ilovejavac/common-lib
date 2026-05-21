package com.dev.lib.aksk.config;

import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskKeyGenerator;
import com.dev.lib.aksk.domain.service.AkskService;
import com.dev.lib.aksk.domain.service.AkskServiceImpl;
import com.dev.lib.aksk.domain.service.AkskSigner;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.aksk.trigger.AkskAdminController;
import com.dev.lib.aksk.web.AkskAuthenticationInterceptor;
import com.dev.lib.aksk.web.AkskAuthenticationSuccessHandler;
import com.dev.lib.aksk.web.AkskMvcInterceptorRegistration;
import com.dev.lib.aksk.web.AkskRequestBodyFilter;
import io.github.linpeilie.Converter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;

@AutoConfiguration
public class AkskAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "app.aksk")
    public AkskProperties akskProperties() {

        return new AkskProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public AkskKeyGenerator akskKeyGenerator() {

        return new AkskKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AkskHeaderResolver akskHeaderResolver() {

        return new AkskHeaderResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AkskSigner akskSigner() {

        return new AkskSigner();
    }

    @Bean
    @ConditionalOnMissingBean(AkskService.class)
    public AkskService akskService(
            AkskCredential.Mapper mapper,
            AkskKeyGenerator keyGenerator,
            Converter converter
    ) {

        return new AkskServiceImpl(mapper, keyGenerator, converter);
    }

    @Bean
    @ConditionalOnMissingBean
    public AkskVerifier akskVerifier(
            AkskService service,
            AkskSigner signer,
            AkskProperties properties
    ) {

        return new AkskVerifier(service, signer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AkskAdminController akskAdminController(AkskService service) {

        return new AkskAdminController(service);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HandlerInterceptor.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public AkskAuthenticationInterceptor akskAuthenticationInterceptor(
            AkskVerifier verifier,
            AkskHeaderResolver headerResolver,
            AkskProperties properties,
            ObjectProvider<AkskAuthenticationSuccessHandler> successHandlers
    ) {

        return new AkskAuthenticationInterceptor(
                verifier,
                headerResolver,
                properties,
                successHandlers.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HandlerInterceptor.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public AkskMvcInterceptorRegistration akskMvcInterceptorRegistration(
            AkskAuthenticationInterceptor akskAuthenticationInterceptor
    ) {

        return new AkskMvcInterceptorRegistration(akskAuthenticationInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public AkskRequestBodyFilter akskRequestBodyFilter(AkskProperties properties) {

        return new AkskRequestBodyFilter(properties);
    }
}
