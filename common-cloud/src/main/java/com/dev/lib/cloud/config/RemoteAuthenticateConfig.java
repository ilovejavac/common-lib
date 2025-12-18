package com.dev.lib.cloud.config;

import com.dev.lib.security.service.AuthenticateService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(AuthenticateService.class)
public class RemoteAuthenticateConfig {

    @DubboReference
    private AuthenticateService remoteService;

    @Bean
    public AuthenticateService authenticateService() {

        return remoteService;
    }

}