package com.dev.lib.security.config;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.service.PermissionService;
import com.dev.lib.security.util.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
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

}