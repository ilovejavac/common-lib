package com.dev.lib.cloud.config;

import com.dev.lib.security.service.AuthenticateService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.dev.lib.security.service.AuthenticateService")
@ConditionalOnMissingBean(type = "com.dev.lib.security.service.AuthenticateService")
public class RemoteAuthenticateConfig {

    @DubboReference
    private AuthenticateService remoteService;

}