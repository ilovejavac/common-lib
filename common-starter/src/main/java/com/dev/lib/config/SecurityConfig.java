package com.dev.lib.config;

import com.dev.lib.security.AuthenticateService;
import com.dev.lib.security.PermissionService;
import com.dev.lib.security.util.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean(PermissionService.class)
    public PermissionService permissionService() {
        return new PermissionService() {
            @Override
            public boolean hasPermission(String... permissions) {
                if (!SecurityContextHolder.isLogin()) {
                    return false;
                }
                return Arrays.stream(permissions).anyMatch(SecurityContextHolder.get()::hasPermission);
            }

            @Override
            public boolean hasRole(String... roles) {
                if (!SecurityContextHolder.isLogin()) {
                    return false;
                }
                return Arrays.stream(roles).anyMatch(SecurityContextHolder.get()::hasRole);
            }
        };
    }

    @Configuration
    @ConditionalOnMissingBean(AuthenticateService.class)
    public static class RemoteAuthenticateConfig {

        @DubboReference
        private AuthenticateService remoteService;

        @Bean
        public AuthenticateService authenticateService() {
            return remoteService;
        }
    }
}