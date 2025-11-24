package com.dev.lib.config;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.AuthenticateService;
import com.dev.lib.security.PermissionService;
import com.dev.lib.security.TokenService;
import com.dev.lib.security.util.AuthenticationFilter;
import com.dev.lib.security.util.JwtUtil;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

    @Bean
    @ConditionalOnBean(AuthenticateService.class)
    public TokenService tokenService(AppSecurityProperties properties, AuthenticateService authService) {
        return new JwtUtil(properties, authService);
    }

    @Bean
    @ConditionalOnBean(TokenService.class)
    public AuthenticationFilter authenticationFilter(TokenService tokenService) {
        return new AuthenticationFilter(tokenService);
    }

    @Bean
    @ConditionalOnMissingBean(TokenService.class)
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }
}